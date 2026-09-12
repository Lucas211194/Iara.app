package com.iara.common.exception;

/**
 * Lancada quando uma operacao de criptografia ou decriptografia de coluna
 * falha (chave ausente/invalida, payload corrompido, falha de integridade
 * da tag de autenticacao GCM, etc.).
 *
 * A mensagem exposta ao chamador NUNCA deve conter a chave, o IV, o texto
 * plano ou o texto cifrado — apenas uma descricao tecnica do tipo de falha.
 */
public class EncryptionException extends IaraException {

    private static final String ERROR_CODE = "ENCRYPTION_FAILURE";

    public EncryptionException(String message) {
        super(ERROR_CODE, message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
