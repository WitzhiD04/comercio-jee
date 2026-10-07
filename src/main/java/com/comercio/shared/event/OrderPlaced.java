package com.comercio.shared.event;

import java.util.UUID;

/** Pedido registrado en PENDIENTE. Su observador AFTER_SUCCESS arranca la SAGA. */
public record OrderPlaced(UUID orderId) {
}
