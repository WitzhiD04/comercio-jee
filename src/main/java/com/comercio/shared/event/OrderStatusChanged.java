package com.comercio.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cambio de estado de un pedido (incluida su creación, con {@code previousStatus == null}).
 * Lleva el resumen completo para que la proyección pueda hacer upsert; {@code version} ordena los eventos.
 */
public record OrderStatusChanged(UUID orderId, String customerId, String previousStatus, String status,
                                 BigDecimal total, int itemsCount, long version, Instant occurredAt) {
}
