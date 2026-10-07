package com.comercio.query.application;

import com.comercio.query.domain.PaymentView;
import com.comercio.query.infrastructure.ReadModelRepository;
import com.comercio.shared.event.PaymentProcessed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.util.logging.Logger;

/** Proyección CQRS de pagos (AFTER_SUCCESS + REQUIRES_NEW). Un pago no cambia una vez registrado. */
@ApplicationScoped
public class PaymentProjector {

    private static final Logger LOG = Logger.getLogger(PaymentProjector.class.getName());

    @Inject
    ReadModelRepository views;

    @Transactional(TxType.REQUIRES_NEW)
    public void onPaymentProcessed(@Observes(during = TransactionPhase.AFTER_SUCCESS) PaymentProcessed e) {
        if (views.findPayment(e.paymentId()).isPresent()) {
            return;
        }
        views.persist(new PaymentView(e.paymentId(), e.orderId(), e.amount(), e.status(), e.reason(),
                e.processedAt()));
        LOG.info(() -> "[CQRS] PaymentView " + e.paymentId() + " pedido=" + e.orderId() + " " + e.status());
    }
}
