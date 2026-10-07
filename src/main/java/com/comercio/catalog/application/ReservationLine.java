package com.comercio.catalog.application;

/** Unidades de un producto que un pedido necesita reservar. */
public record ReservationLine(Long productId, int quantity) {
}
