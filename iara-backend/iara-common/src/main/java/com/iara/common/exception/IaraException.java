package com.iara.common.exception;

/**
 * Excecao base de todo o dominio Iara.
 *
 * Todas as excecoes de negocio do backend devem estender esta classe (ou uma
 * subclasse dela), nunca RuntimeException diretamente. Isso permite que o
 * {@link GlobalExceptionHandler} capture qualquer falha de dominio de forma
 * consistente e devolva um {@link ErrorResponse} padronizado ao cliente, sem
 * vazar detalhes internos (stack trace, nome de classe, mensagem de driver
 * JDBC, etc.) que poderiam expor informacao sensivel do sistema.
 */
public abstract class IaraException extends RuntimeException {

    private final String errorCode;

    protected IaraException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected IaraException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
