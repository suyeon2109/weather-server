package com.gbsoft.weather.exception;

import lombok.Getter;

import java.util.HashMap;

@Getter
public abstract class ApiException extends RuntimeException {
    public final HashMap<String, Object> validation = new HashMap<>();

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract int getStatusCode();

    public void addValidation(String fieldName, String message) {
        validation.put(fieldName, message);
    }

}
