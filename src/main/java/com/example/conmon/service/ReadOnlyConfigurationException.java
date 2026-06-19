package com.example.conmon.service;

public class ReadOnlyConfigurationException extends RuntimeException {
    public ReadOnlyConfigurationException(String message) {
        super(message);
    }
}
