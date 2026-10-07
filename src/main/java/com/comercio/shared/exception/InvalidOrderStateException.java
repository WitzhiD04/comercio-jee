package com.comercio.shared.exception;

public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "INVALID_ORDER_STATE";
    }
}
