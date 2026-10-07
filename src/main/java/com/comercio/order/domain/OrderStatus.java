package com.comercio.order.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del pedido y su tabla de transiciones permitidas.
 *
 * <pre>
 * PENDIENTE ──► INVENTARIO_RESERVADO ──► PAGO_APROBADO ──► CONFIRMADO
 *     │                  │
 *     │                  └──► PAGO_RECHAZADO ──► INVENTARIO_LIBERADO ──► CANCELADO
 *     └──────────────────────────────────────────────────────────────► CANCELADO  (sin stock)
 * </pre>
 */
public enum OrderStatus {
    PENDIENTE,
    INVENTARIO_RESERVADO,
    PAGO_APROBADO,
    CONFIRMADO,
    PAGO_RECHAZADO,
    INVENTARIO_LIBERADO,
    CANCELADO;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDIENTE, EnumSet.of(INVENTARIO_RESERVADO, CANCELADO),
            INVENTARIO_RESERVADO, EnumSet.of(PAGO_APROBADO, PAGO_RECHAZADO),
            PAGO_APROBADO, EnumSet.of(CONFIRMADO),
            PAGO_RECHAZADO, EnumSet.of(INVENTARIO_LIBERADO),
            INVENTARIO_LIBERADO, EnumSet.of(CANCELADO),
            CONFIRMADO, EnumSet.noneOf(OrderStatus.class),
            CANCELADO, EnumSet.noneOf(OrderStatus.class));

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public Set<OrderStatus> allowedTransitions() {
        return Set.copyOf(TRANSITIONS.get(this));
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
