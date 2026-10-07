package com.comercio.shared.exception;

import java.util.UUID;

public class PaymentRejectedException extends DomainException {

    public PaymentRejectedException(UUID orderId, String reason) {
        super("Pago rechazado para el pedido " + orderId + ": " + reason);
    }

    @Override
    public String errorCode() {
        return "PAYMENT_REJECTED";
    }
}
