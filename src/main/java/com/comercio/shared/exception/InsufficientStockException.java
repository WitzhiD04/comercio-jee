package com.comercio.shared.exception;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(Long productId, int requested, int available) {
        this("Stock insuficiente para el producto " + productId
                + ": solicitado " + requested + ", disponible " + available);
    }

    @Override
    public String errorCode() {
        return "INSUFFICIENT_STOCK";
    }
}
