package com.comercio.query.application;

import com.comercio.query.domain.OrderSummaryView;
import com.comercio.query.infrastructure.ReadModelRepository;
import com.comercio.shared.exception.OrderNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/** Consultas de pedidos. Solo leen queryPU. */
@ApplicationScoped
public class OrderQueryService {

    private static final int MAX_RESULTS = 100;

    @Inject
    ReadModelRepository views;

    public OrderSummaryView getOrder(UUID orderId) {
        return views.findOrder(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /** Pedidos de un cliente; sin cliente, los más recientes. */
    public List<OrderSummaryView> listOrders(String customerId) {
        return customerId == null || customerId.isBlank()
                ? views.findRecentOrders(MAX_RESULTS)
                : views.findOrdersByCustomer(customerId);
    }
}
