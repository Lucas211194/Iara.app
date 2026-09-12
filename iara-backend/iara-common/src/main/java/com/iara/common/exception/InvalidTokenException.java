package com.iara.common.exception;

/**
 * Lancada quando um JWT (access ou refresh token) e invalido: assinatura
 * incorreta, expirado, malformado, emissor divergente ou tipo de token
 * inesperado (ex.: um refresh token sendo usado como access token).
 */
public class InvalidTokenException extends IaraException {

    private static final String ERROR_CODE = "INVALID_TOKEN";

    public InvalidTokenException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
