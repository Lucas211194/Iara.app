package com.iara.common.crypto;

import com.iara.common.exception.EncryptionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia em nivel de coluna (Column-Level Encryption) usando AES-GCM
 * (AEAD: confidencialidade + integridade + autenticidade em uma unica
 * operacao).
 *
 * Formato armazenado (Base64 de um unico blob binario):
 *   [ IV (12 bytes) ][ ciphertext ][ auth tag GCM (16 bytes, anexado pelo JDK) ]
 *
 * Requisitos de seguranca implementados:
 * - A chave NUNCA fica hardcoded no codigo-fonte. E lida exclusivamente da
 *   variavel de ambiente IARA_COLUMN_ENCRYPTION_KEY (ver application.yml:
 *   iara.crypto.column-encryption-key: ${IARA_COLUMN_ENCRYPTION_KEY}).
 * - A chave deve ser uma string Base64 de exatamente 32 bytes decodificados
 *   (AES-256). Chave de tamanho invalido falha no boot da aplicacao
 *   (fail-fast), nunca silenciosamente com uma chave fraca.
 * - Um IV (nonce) aleatorio de 96 bits e gerado a cada chamada de encrypt().
 *   GCM exige que o par (chave, IV) nunca se repita; reutilizar IV com a
 *   mesma chave quebra a confidencialidade. SecureRandom garante
 *   imprevisibilidade criptografica.
 * - A tag de autenticacao GCM (128 bits) detecta qualquer adulteracao do
 *   ciphertext armazenado; decrypt() falha com EncryptionException se o
 *   dado foi corrompido/adulterado, em vez de retornar lixo silenciosamente.
 */
@Component
public class ColumnEncryptor implements InitializingBean {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int REQUIRED_KEY_LENGTH_BYTES = 32;

    private final String rawBase64Key;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec secretKey;

    public ColumnEncryptor(@Value("${iara.crypto.column-encryption-key:}") String base64Key) {
        this.rawBase64Key = base64Key;
    }

    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(rawBase64Key)) {
            throw new IllegalStateException(
                "Variavel de ambiente IARA_COLUMN_ENCRYPTION_KEY nao definida. " +
                "Gere uma chave AES-256 com: openssl rand -base64 32");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(rawBase64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "IARA_COLUMN_ENCRYPTION_KEY nao e um Base64 valido.", e);
        }
        if (decoded.length != REQUIRED_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                "IARA_COLUMN_ENCRYPTION_KEY deve decodificar para exatamente " +
                REQUIRED_KEY_LENGTH_BYTES + " bytes (AES-256). Tamanho atual: " +
                decoded.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(decoded, KEY_ALGORITHM);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        requireInitialized();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Falha ao criptografar dado de coluna.", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) {
            return null;
        }
        requireInitialized();
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            if (decoded.length < GCM_IV_LENGTH_BYTES) {
                throw new EncryptionException("Payload cifrado menor que o IV minimo esperado.");
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (EncryptionException e) {
            throw e;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new EncryptionException("Falha ao decriptografar dado de coluna: payload invalido ou corrompido.", e);
        }
    }

    private void requireInitialized() {
        if (secretKey == null) {
            throw new IllegalStateException("ColumnEncryptor nao foi inicializado corretamente.");
        }
    }
}
