package com.comercio.saga.application;

import com.comercio.catalog.application.ReservationLine;
import com.comercio.order.domain.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Datos del pedido que el orquestador necesita para dirigir la SAGA (copia inmutable, no la entidad). */
public record SagaOrderData(UUID orderId, List<ReservationLine> items, BigDecimal total, String paymentMethod) {

    static SagaOrderData from(Order order) {
        return new SagaOrderData(order.getId(),
                order.getItems().stream().map(i -> new ReservationLine(i.getProductId(), i.getQuantity())).toList(),
                order.getTotal(), order.getPaymentMethod());
    }
}
