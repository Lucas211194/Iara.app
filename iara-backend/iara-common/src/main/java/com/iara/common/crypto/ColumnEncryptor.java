package com.iara.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia em nível de coluna (Column-Level Encryption) usando AES-GCM.
 * 
 * REGRAS DE PRIVACIDADE IARA:
 * - Campos sensíveis (saúde reprodutiva, peso, anotações) SEMPRE criptografados em coluna
 * - Chave mestra derivada de HSM/KMS em produção; aqui usa chave derivada de config
 * - Nonce único por criptografia (GCM) - nunca reutiliza nonce
 * - Tag de autenticação GCM integrada no payload criptografado
 */
@Component
@Slf4j
public class ColumnEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits para GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_SIZE = 256;

    private final SecretKey masterKey;

    public ColumnEncryptor(@Value("${iara.crypto.column-encryption-key}") String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        this.masterKey = new SecretKeySpec(decoded, 0, decoded.length, "AES");
    }

    /**
     * Criptografa valor sensível para armazenamento em coluna.
     * Formato armazenado: [IV (12 bytes)][Ciphertext][AuthTag (16 bytes)] em Base64
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + Ciphertext + AuthTag (GCM appends tag automatically)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Falha na criptografia de coluna", e);
            throw new ColumnEncryptionException("Falha ao criptografar dado sensível", e);
        }
    }

    /**
     * Descriptografa valor da coluna.
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Falha na descriptografia de coluna - dado pode estar corrompido ou chave incorreta", e);
            throw new ColumnEncryptionException("Falha ao descriptografar dado sensível", e);
        }
    }

    /**
     * Converter JPA para uso transparente em entidades.
     */
    @Converter
    public static class EncryptedStringConverter implements AttributeConverter<String, String> {
        private final ColumnEncryptor encryptor;

        public EncryptedStringConverter(ColumnEncryptor encryptor) {
            this.encryptor = encryptor;
        }

        @Override
        public String convertToDatabaseColumn(String plaintext) {
            return encryptor.encrypt(plaintext);
        }

        @Override
        public String convertToEntityAttribute(String encrypted) {
            return encryptor.decrypt(encrypted);
        }
    }
}