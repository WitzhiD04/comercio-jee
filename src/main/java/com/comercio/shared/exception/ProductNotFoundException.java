package com.comercio.shared.exception;

public class ProductNotFoundException extends DomainException {

    public ProductNotFoundException(Long productId) {
        super("No existe el producto con id " + productId);
    }

    @Override
    public String errorCode() {
        return "PRODUCT_NOT_FOUND";
    }
}
