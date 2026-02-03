package com.mindlog.domain.diary.exception;

public class DuplicateDiaryDateException extends RuntimeException {
    public DuplicateDiaryDateException(String message) {
        super(message);
    }
}
