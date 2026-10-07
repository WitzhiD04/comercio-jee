package com.comercio.query.application;

import com.comercio.query.domain.PaymentView;
import com.comercio.query.infrastructure.ReadModelRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/** Consultas de pagos. Solo leen queryPU. */
@ApplicationScoped
public class PaymentQueryService {

    private static final int MAX_RESULTS = 100;

    @Inject
    ReadModelRepository views;

    /** Pagos de un pedido; sin pedido, los más recientes. */
    public List<PaymentView> listPayments(UUID orderId) {
        return orderId == null ? views.findRecentPayments(MAX_RESULTS) : views.findPaymentsByOrder(orderId);
    }
}
