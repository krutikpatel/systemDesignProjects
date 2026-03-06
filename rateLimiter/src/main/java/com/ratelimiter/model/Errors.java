package com.ratelimiter.model;

import java.util.List;

public final class Errors {
    private Errors() {
    }

    public static class StoreException extends RuntimeException {
        public StoreException(String message) {
            super(message);
        }

        public StoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PolicyNotFoundException extends RuntimeException {
        public PolicyNotFoundException(String message) {
            super(message);
        }
    }

    public static class ConfigValidationException extends RuntimeException {
        private final List<String> errors;

        public ConfigValidationException(List<String> errors) {
            super(String.join("; ", errors));
            this.errors = List.copyOf(errors);
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static class UnknownAlgorithmException extends RuntimeException {
        public UnknownAlgorithmException(String message) {
            super(message);
        }
    }

    public static class RateLimiterUnavailableException extends RuntimeException {
        public RateLimiterUnavailableException(String message) {
            super(message);
        }

        public RateLimiterUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
