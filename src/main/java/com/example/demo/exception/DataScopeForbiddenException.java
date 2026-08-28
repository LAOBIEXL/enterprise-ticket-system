package com.example.demo.exception;

public class DataScopeForbiddenException extends RuntimeException {
    public DataScopeForbiddenException(String message) {
        super(message);
    }
}
