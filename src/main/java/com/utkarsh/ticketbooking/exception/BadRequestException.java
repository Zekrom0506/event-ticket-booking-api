package com.utkarsh.ticketbooking.exception;

// Thrown when the request is not valid for the current state (returns 400)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
