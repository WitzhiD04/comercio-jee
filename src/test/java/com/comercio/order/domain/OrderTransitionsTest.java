package com.comercio.order.domain;

import com.comercio.shared.exception.InvalidOrderStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import static com.comercio.order.domain.OrderStatus.CANCELADO;
import static com.comercio.order.domain.OrderStatus.CONFIRMADO;
import static com.comercio.order.domain.OrderStatus.INVENTARIO_LIBERADO;
import static com.comercio.order.domain.OrderStatus.INVENTARIO_RESERVADO;
import static com.comercio.order.domain.OrderStatus.PAGO_APROBADO;
import static com.comercio.order.domain.OrderStatus.PAGO_RECHAZADO;
import static com.comercio.order.domain.OrderStatus.PENDIENTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTransitionsTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("cliente-1", "TARJETA");
        order.addItem(1L, 2, new BigDecimal("1000.00"));
        order.addItem(2L, 1, new BigDecimal("500.50"));
    }

    @Test
    @DisplayName("Un pedido nuevo nace en PENDIENTE con el total calculado")
    void newOrderIsPending() {
        assertEquals(PENDIENTE, order.getStatus());
        assertEquals(new BigDecimal("2500.50"), order.getTotal());
        assertEquals(3, order.itemsCount());
    }

    @Test
    @DisplayName("Flujo feliz: PENDIENTE → INVENTARIO_RESERVADO → PAGO_APROBADO → CONFIRMADO")
    void happyPath() {
        order.transitionTo(INVENTARIO_RESERVADO);
        order.transitionTo(PAGO_APROBADO);
        order.transitionTo(CONFIRMADO);

        assertEquals(CONFIRMADO, order.getStatus());
        assertTrue(order.getStatus().isTerminal());
    }

    @Test
    @DisplayName("Compensación: INVENTARIO_RESERVADO → PAGO_RECHAZADO → INVENTARIO_LIBERADO → CANCELADO")
    void compensationPath() {
        order.transitionTo(INVENTARIO_RESERVADO);
        order.transitionTo(PAGO_RECHAZADO);
        order.transitionTo(INVENTARIO_LIBERADO);
        order.transitionTo(CANCELADO);

        assertEquals(CANCELADO, order.getStatus());
    }

    @Test
    @DisplayName("Sin stock: PENDIENTE → CANCELADO directamente")
    void outOfStockPath() {
        order.transitionTo(CANCELADO);

        assertEquals(CANCELADO, order.getStatus());
    }

    @Test
    @DisplayName("No se puede confirmar un pedido sin pago aprobado")
    void cannotSkipPayment() {
        order.transitionTo(INVENTARIO_RESERVADO);

        InvalidOrderStateException ex = assertThrows(InvalidOrderStateException.class,
                () -> order.transitionTo(CONFIRMADO));
        assertTrue(ex.getMessage().contains("INVENTARIO_RESERVADO -> CONFIRMADO"));
        assertEquals(INVENTARIO_RESERVADO, order.getStatus(), "una transición inválida no cambia el estado");
    }

    @Test
    @DisplayName("Un pago rechazado no puede cancelarse sin liberar antes el inventario")
    void rejectedPaymentMustReleaseInventoryFirst() {
        order.transitionTo(INVENTARIO_RESERVADO);
        order.transitionTo(PAGO_RECHAZADO);

        assertThrows(InvalidOrderStateException.class, () -> order.transitionTo(CANCELADO));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CONFIRMADO", "CANCELADO"})
    @DisplayName("Los estados terminales no admiten ninguna transición")
    void terminalStatesAreFinal(OrderStatus terminal) {
        assertTrue(terminal.isTerminal());
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(terminal.canTransitionTo(target), terminal + " -> " + target);
        }
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("Ningún estado puede transicionar a sí mismo ni volver a PENDIENTE")
    void noSelfTransitionsNorBackToPending(OrderStatus status) {
        assertFalse(status.canTransitionTo(status));
        assertFalse(status.canTransitionTo(PENDIENTE));
    }

    @Test
    @DisplayName("La tabla de transiciones es exactamente la documentada")
    void transitionTable() {
        assertEquals(EnumSet.of(INVENTARIO_RESERVADO, CANCELADO), PENDIENTE.allowedTransitions());
        assertEquals(EnumSet.of(PAGO_APROBADO, PAGO_RECHAZADO), INVENTARIO_RESERVADO.allowedTransitions());
        assertEquals(Set.of(CONFIRMADO), PAGO_APROBADO.allowedTransitions());
        assertEquals(Set.of(INVENTARIO_LIBERADO), PAGO_RECHAZADO.allowedTransitions());
        assertEquals(Set.of(CANCELADO), INVENTARIO_LIBERADO.allowedTransitions());
    }
}
