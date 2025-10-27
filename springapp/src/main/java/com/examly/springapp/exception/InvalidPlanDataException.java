package com.examly.springapp.exception;

public class InvalidPlanDataException extends RuntimeException {
    public InvalidPlanDataException(String message) {
        super(message);
    }
}
