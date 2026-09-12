package com.iara.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tratamento global de excecoes para todos os modulos que expoem endpoints
 * REST e importam este modulo common.
 *
 * Regra de privacidade: nenhuma excecao aqui deve devolver ao cliente o
 * stack trace, a mensagem original de excecoes de infraestrutura (JDBC,
 * Hibernate, etc.) ou qualquer valor de dado sensivel. O log interno (via
 * SLF4J) pode registrar detalhes tecnicos, mas nunca o conteudo de campos
 * anotados com @SensitiveHealthData.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SensitiveDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleSensitiveDataAccess(SensitiveDataAccessException ex,
                                                                     HttpServletRequest request) {
        log.warn("Acesso negado a dado sensivel: errorCode={}, path={}", ex.getErrorCode(), request.getRequestURI());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex,
                                                              HttpServletRequest request) {
        log.debug("Token invalido: errorCode={}, path={}", ex.getErrorCode(), request.getRequestURI());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> handleEncryption(EncryptionException ex,
                                                            HttpServletRequest request) {
        log.error("Falha de criptografia em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getErrorCode(),
                "Nao foi possivel processar os dados protegidos no momento.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(IaraException.class)
    public ResponseEntity<ErrorResponse> handleIaraException(IaraException ex, HttpServletRequest request) {
        log.warn("Erro de dominio: errorCode={}, path={}", ex.getErrorCode(), request.getRequestURI());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Um ou mais campos sao invalidos.",
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Ocorreu um erro inesperado. Tente novamente em instantes.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
