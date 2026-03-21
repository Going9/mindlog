package com.mindlog.global.exception;

public class SupabaseAuthException extends RuntimeException {

    public SupabaseAuthException(String message) {
        super(message);
    }

    public SupabaseAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
