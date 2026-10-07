package com.comercio.shared.event;

import java.time.Instant;

/** Cambio de existencias o de unidades reservadas de un producto (reserva, liberación, confirmación, ajuste). */
public record StockChanged(Long productId, int stock, int reserved, long version, String cause, Instant occurredAt) {
}
