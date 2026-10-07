package com.comercio.shared.exception;

import java.util.UUID;

public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(UUID orderId) {
        super("No existe el pedido " + orderId);
    }

    @Override
    public String errorCode() {
        return "ORDER_NOT_FOUND";
    }
}
