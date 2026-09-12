package com.iara.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter JPA transparente para campos de texto que devem ser
 * criptografados em coluna (AES-GCM via {@link ColumnEncryptor}).
 *
 * Uso em uma entidade:
 * <pre>
 *   {@literal @}Convert(converter = EncryptedStringConverter.class)
 *   {@literal @}SensitiveHealthData(category = SensitiveHealthData.Category.PERSONAL_NOTE)
 *   private String observacoes;
 * </pre>
 *
 * autoApply = false: a criptografia deve ser uma decisao explicita por
 * campo (via @Convert), nunca implicita para todo o tipo String da
 * aplicacao — isso evitaria criptografar campos que nao sao sensiveis
 * (ex.: um titulo de tela) e prejudicaria buscas/indices desnecessariamente.
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private ColumnEncryptor encryptor;

    private ColumnEncryptor encryptor() {
        if (encryptor == null) {
            encryptor = ApplicationContextProvider.getBean(ColumnEncryptor.class);
        }
        return encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptor().decrypt(dbData);
    }
}
