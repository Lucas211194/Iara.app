package com.iara.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Formato padronizado de resposta de erro devolvido pela API Iara.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String errorCode;
    private final String message;
    private final String path;
    private final List<FieldError> fieldErrors;

    public ErrorResponse(int status, String errorCode, String message, String path) {
        this(status, errorCode, message, path, null);
    }

    public ErrorResponse(int status, String errorCode, String message, String path, List<FieldError> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldError)) return false;
            FieldError that = (FieldError) o;
            return Objects.equals(field, that.field) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, message);
        }
    }
}
