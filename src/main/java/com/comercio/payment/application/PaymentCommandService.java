package com.comercio.payment.application;

import com.comercio.payment.domain.Payment;
import com.comercio.payment.infrastructure.PaymentRepository;
import com.comercio.payment.infrastructure.SimulatedPaymentGateway;
import com.comercio.payment.infrastructure.SimulatedPaymentGateway.GatewayResponse;
import com.comercio.shared.event.PaymentProcessed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Cobro de pedidos (paymentsPU). {@code MANDATORY}: corre dentro de la transacción JTA del paso de la
 * SAGA, junto con el cambio de estado del pedido en ordersPU.
 */
@ApplicationScoped
@Transactional(TxType.MANDATORY)
public class PaymentCommandService {

    private static final Logger LOG = Logger.getLogger(PaymentCommandService.class.getName());

    @Inject
    PaymentRepository payments;

    @Inject
    SimulatedPaymentGateway gateway;

    @Inject
    Event<PaymentProcessed> paymentProcessed;

    /**
     * Idempotente por pedido: si ya existe un pago para {@code orderId} se devuelve tal cual, sin volver a
     * llamar a la pasarela. Tanto el pago aprobado como el rechazado quedan registrados.
     */
    public Payment process(UUID orderId, BigDecimal amount, String paymentMethod) {
        var existing = payments.findByOrderId(orderId);
        if (existing.isPresent()) {
            LOG.info(() -> "Pago ya registrado para el pedido " + orderId + " (idempotencia)");
            return existing.get();
        }
        GatewayResponse response = gateway.charge(orderId, amount, paymentMethod);
        Payment payment = response.approved()
                ? Payment.approved(orderId, amount)
                : Payment.rejected(orderId, amount, response.reason());
        payments.save(payment);
        paymentProcessed.fire(new PaymentProcessed(payment.getId(), orderId, amount, payment.getStatus().name(),
                payment.getReason(), payment.getProcessedAt()));
        return payment;
    }
}
