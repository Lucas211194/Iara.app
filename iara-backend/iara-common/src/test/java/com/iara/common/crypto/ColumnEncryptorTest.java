package com.iara.common.crypto;

import com.iara.common.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes reais (nao triviais) do ColumnEncryptor: cobrem round-trip de
 * criptografia/decriptografia, unicidade de IV entre chamadas, deteccao de
 * adulteracao (integridade GCM) e fail-fast em chave invalida/ausente.
 */
class ColumnEncryptorTest {

    private static final String VALID_BASE64_KEY = generateValidBase64Key();

    private ColumnEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new ColumnEncryptor(VALID_BASE64_KEY);
        encryptor.afterPropertiesSet();
    }

    @Test
    void encryptThenDecrypt_returnsOriginalPlaintext() {
        String plaintext = "Sangramento intenso, dor pelvica 8/10 - anotacao pessoal da usuaria";

        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_neverReturnsPlaintextAsSubstring() {
        String plaintext = "endometriose confirmada em exame";

        String encrypted = encryptor.encrypt(plaintext);

        assertThat(encrypted).doesNotContain("endometriose");
    }

    @Test
    void encrypt_producesDifferentCiphertextForSamePlaintext_dueToRandomIv() {
        String plaintext = "mesmo texto";

        String encrypted1 = encryptor.encrypt(plaintext);
        String encrypted2 = encryptor.encrypt(plaintext);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void encrypt_generatesUniqueIvsAcrossManyOperations() {
        Set<String> ivPrefixes = new HashSet<>();
        String plaintext = "texto de teste para unicidade de IV";

        for (int i = 0; i < 200; i++) {
            String encrypted = encryptor.encrypt(plaintext);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            String ivPrefix = Base64.getEncoder().encodeToString(
                    java.util.Arrays.copyOfRange(decoded, 0, 12));
            ivPrefixes.add(ivPrefix);
        }

        assertThat(ivPrefixes).hasSize(200);
    }

    @Test
    void decrypt_detectsTamperedCiphertext_andThrowsEncryptionException() {
        String encrypted = encryptor.encrypt("dado que sera adulterado");

        byte[] tampered = Base64.getDecoder().decode(encrypted);
        tampered[tampered.length - 1] ^= 0x01;
        String tamperedBase64 = Base64.getEncoder().encodeToString(tampered);

        assertThatThrownBy(() -> encryptor.decrypt(tamperedBase64))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void decrypt_withWrongKey_throwsEncryptionException() {
        String encrypted = encryptor.encrypt("dado protegido");

        ColumnEncryptor otherEncryptor = new ColumnEncryptor(generateValidBase64Key());
        otherEncryptor.afterPropertiesSet();

        assertThatThrownBy(() -> otherEncryptor.decrypt(encrypted))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void encrypt_withNullInput_returnsNull() {
        assertThat(encryptor.encrypt(null)).isNull();
    }

    @Test
    void decrypt_withNullInput_returnsNull() {
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void afterPropertiesSet_withMissingKey_throwsIllegalStateException() {
        ColumnEncryptor missingKeyEncryptor = new ColumnEncryptor("");

        assertThatThrownBy(missingKeyEncryptor::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IARA_COLUMN_ENCRYPTION_KEY");
    }

    @Test
    void afterPropertiesSet_withWrongKeyLength_throwsIllegalStateException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        ColumnEncryptor shortKeyEncryptor = new ColumnEncryptor(shortKey);

        assertThatThrownBy(shortKeyEncryptor::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void afterPropertiesSet_withInvalidBase64_throwsIllegalStateException() {
        ColumnEncryptor invalidEncryptor = new ColumnEncryptor("nao-e-base64-valido!!!");

        assertThatThrownBy(invalidEncryptor::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class);
    }

    private static String generateValidBase64Key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
