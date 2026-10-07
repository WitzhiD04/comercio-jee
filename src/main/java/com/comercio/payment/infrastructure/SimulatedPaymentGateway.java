package com.comercio.payment.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Pasarela de pagos simulada. Rechaza si el medio de pago es {@code "FAIL"} o si el monto supera
 * 5.000.000; en cualquier otro caso aprueba.
 */
@ApplicationScoped
public class SimulatedPaymentGateway {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("5000000");

    private static final Logger LOG = Logger.getLogger(SimulatedPaymentGateway.class.getName());

    public record GatewayResponse(boolean approved, String reason) {
    }

    public GatewayResponse charge(UUID orderId, BigDecimal amount, String paymentMethod) {
        GatewayResponse response;
        if ("FAIL".equalsIgnoreCase(paymentMethod)) {
            response = new GatewayResponse(false, "Medio de pago rechazado por la pasarela");
        } else if (amount.compareTo(MAX_AMOUNT) > 0) {
            response = new GatewayResponse(false, "El monto " + amount.toPlainString()
                    + " supera el máximo permitido de " + MAX_AMOUNT.toPlainString());
        } else {
            response = new GatewayResponse(true, "Pago aprobado");
        }
        LOG.info(() -> "Pasarela: pedido=" + orderId + " monto=" + amount + " medio=" + paymentMethod
                + " -> " + (response.approved() ? "APROBADO" : "RECHAZADO"));
        return response;
    }
}
