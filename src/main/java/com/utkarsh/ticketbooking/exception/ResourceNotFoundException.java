package com.utkarsh.ticketbooking.exception;

// Thrown when an event, booking or user does not exist (returns 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
