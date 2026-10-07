package com.comercio.shared.exception;

public class DuplicateSkuException extends DomainException {

    public DuplicateSkuException(String sku) {
        super("Ya existe un producto con sku " + sku);
    }

    @Override
    public String errorCode() {
        return "DUPLICATE_SKU";
    }
}
