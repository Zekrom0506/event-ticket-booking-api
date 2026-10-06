package com.utkarsh.ticketbooking.exception;

// Thrown when there are no seats left for an event (returns 409)
public class SoldOutException extends RuntimeException {

    public SoldOutException(String message) {
        super(message);
    }
}
