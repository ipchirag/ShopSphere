package com.chirag.shopsphere.exception;

public class InvalidStatusTransitionException extends BadRequestException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Invalid status transition from '%s' to '%s'", currentStatus, targetStatus));
    }
}
