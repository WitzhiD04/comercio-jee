package com.comercio.saga.application;

import com.comercio.catalog.application.ReservationLine;
import com.comercio.saga.domain.SagaStatus;
import com.comercio.saga.domain.StepAction;
import com.comercio.shared.exception.InsufficientStockException;
import com.comercio.shared.exception.PaymentRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.comercio.saga.application.OrderSagaSteps.CONFIRMAR_PEDIDO;
import static com.comercio.saga.application.OrderSagaSteps.LIBERAR_INVENTARIO;
import static com.comercio.saga.application.OrderSagaSteps.RESERVAR_INVENTARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas síncronas del orquestador: se llama a {@code execute} directamente (sin executor) y los pasos
 * transaccionales se sustituyen por un mock de {@link OrderSagaSteps}.
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final List<ReservationLine> ITEMS = List.of(new ReservationLine(1L, 2), new ReservationLine(3L, 1));
    private static final BigDecimal TOTAL = new BigDecimal("7180000.00");

    @Mock
    OrderSagaSteps steps;

    OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(steps, 0);
        when(steps.start(ORDER_ID)).thenReturn(new SagaOrderData(ORDER_ID, ITEMS, TOTAL, "TARJETA"));
    }

    @Test
    @DisplayName("Éxito: reservar → pagar → confirmar, sin compensaciones; la SAGA queda COMPLETADA")
    void happyPath() {
        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPLETADA, result);
        InOrder order = inOrder(steps);
        order.verify(steps).start(ORDER_ID);
        order.verify(steps).reserveInventory(ORDER_ID, ITEMS);
        order.verify(steps).processPayment(ORDER_ID, TOTAL);
        order.verify(steps).confirmOrder(ORDER_ID);
        verify(steps, never()).releaseInventory(any());
        verify(steps, never()).cancelOrder(any(), anyString());
        verify(steps, never()).markFailed(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Pago rechazado: se compensa en orden inverso (liberar inventario y luego cancelar pedido)")
    void paymentRejectedCompensatesInReverseOrder() {
        doThrow(new PaymentRejectedException(ORDER_ID, "Medio de pago rechazado"))
                .when(steps).processPayment(ORDER_ID, TOTAL);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPENSADA, result);
        InOrder order = inOrder(steps);
        order.verify(steps).reserveInventory(ORDER_ID, ITEMS);   // T2
        order.verify(steps).processPayment(ORDER_ID, TOTAL);     // T3 falla
        order.verify(steps).releaseInventory(ORDER_ID);          // C2 (compensa T2)
        order.verify(steps).cancelOrder(eq(ORDER_ID), anyString()); // C1 (compensa T1)
        order.verifyNoMoreInteractions();
        verify(steps, never()).confirmOrder(any());
        // el rechazo ya quedó en la bitácora dentro de la transacción del pago: el orquestador no lo duplica
        verify(steps, never()).recordStepFailure(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Sin stock: no se cobra ni se libera inventario; solo se cancela el pedido (COMPENSADA)")
    void outOfStockCancelsWithoutCompensatingInventory() {
        doThrow(new InsufficientStockException(1L, 2, 0)).when(steps).reserveInventory(ORDER_ID, ITEMS);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPENSADA, result);
        verify(steps, times(1)).reserveInventory(ORDER_ID, ITEMS); // error de negocio: no se reintenta
        verify(steps).recordStepFailure(eq(ORDER_ID), eq(RESERVAR_INVENTARIO), eq(StepAction.EJECUTAR),
                contains("Stock insuficiente"));
        verify(steps).cancelOrder(eq(ORDER_ID), anyString());
        verify(steps, never()).processPayment(any(), any());
        verify(steps, never()).releaseInventory(any());
        verify(steps, never()).confirmOrder(any());
    }

    @Test
    @DisplayName("Una compensación que falla de forma transitoria se reintenta y la SAGA termina COMPENSADA")
    void compensationIsRetried() {
        doThrow(new PaymentRejectedException(ORDER_ID, "FAIL")).when(steps).processPayment(ORDER_ID, TOTAL);
        doThrow(new IllegalStateException("conexión perdida"))
                .doThrow(new IllegalStateException("conexión perdida"))
                .doNothing()
                .when(steps).releaseInventory(ORDER_ID);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPENSADA, result);
        verify(steps, times(3)).releaseInventory(ORDER_ID);
        verify(steps, times(2)).recordStepFailure(eq(ORDER_ID), eq(LIBERAR_INVENTARIO), eq(StepAction.COMPENSAR),
                anyString());
        verify(steps).cancelOrder(eq(ORDER_ID), anyString());
    }

    @Test
    @DisplayName("Si una compensación sigue fallando tras 3 reintentos, la SAGA queda FALLIDA")
    void compensationExhaustsRetries() {
        doThrow(new PaymentRejectedException(ORDER_ID, "FAIL")).when(steps).processPayment(ORDER_ID, TOTAL);
        doThrow(new IllegalStateException("inventory_db no disponible")).when(steps).releaseInventory(ORDER_ID);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.FALLIDA, result);
        verify(steps, times(OrderSagaOrchestrator.MAX_RETRIES + 1)).releaseInventory(ORDER_ID);
        verify(steps).markFailed(eq(ORDER_ID), eq(LIBERAR_INVENTARIO), eq(StepAction.COMPENSAR), anyString());
        verify(steps, never()).cancelOrder(any(), anyString()); // no se sigue compensando sobre un estado incierto
    }

    @Test
    @DisplayName("Fallo técnico transitorio al reservar: se reintenta y la SAGA continúa")
    void transientReservationFailureIsRetried() {
        doThrow(new IllegalStateException("deadlock detectado"))
                .doNothing()
                .when(steps).reserveInventory(ORDER_ID, ITEMS);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPLETADA, result);
        verify(steps, times(2)).reserveInventory(ORDER_ID, ITEMS);
        verify(steps).confirmOrder(ORDER_ID);
    }

    @Test
    @DisplayName("Fallo técnico en el pago: el pedido pasa a PAGO_RECHAZADO y se compensa")
    void technicalPaymentFailureCompensates() {
        doThrow(new IllegalStateException("payments_db no disponible")).when(steps).processPayment(ORDER_ID, TOTAL);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.COMPENSADA, result);
        verify(steps, times(OrderSagaOrchestrator.MAX_RETRIES + 1)).processPayment(ORDER_ID, TOTAL);
        InOrder order = inOrder(steps);
        order.verify(steps).markPaymentFailed(eq(ORDER_ID), anyString());
        order.verify(steps).releaseInventory(ORDER_ID);
        order.verify(steps).cancelOrder(eq(ORDER_ID), anyString());
    }

    @Test
    @DisplayName("Después del pivote (pago aprobado) no se compensa: si confirmar falla siempre, FALLIDA")
    void confirmFailureAfterPivotIsNotCompensated() {
        doThrow(new IllegalStateException("timeout")).when(steps).confirmOrder(ORDER_ID);

        SagaStatus result = orchestrator.execute(ORDER_ID);

        assertEquals(SagaStatus.FALLIDA, result);
        verify(steps, times(OrderSagaOrchestrator.MAX_RETRIES + 1)).confirmOrder(ORDER_ID);
        verify(steps).markFailed(eq(ORDER_ID), eq(CONFIRMAR_PEDIDO), eq(StepAction.EJECUTAR), anyString());
        verify(steps, never()).releaseInventory(any());
        verify(steps, never()).cancelOrder(any(), anyString());
    }

    @Test
    @DisplayName("Si falla el registro en la bitácora, la compensación continúa igualmente")
    void bookkeepingFailureDoesNotStopCompensation() {
        doThrow(new InsufficientStockException(1L, 2, 0)).when(steps).reserveInventory(ORDER_ID, ITEMS);
        doThrow(new IllegalStateException("orders_db lenta"))
                .when(steps).recordStepFailure(any(), anyString(), any(), anyString());
        doNothing().when(steps).cancelOrder(eq(ORDER_ID), anyString());

        assertEquals(SagaStatus.COMPENSADA, orchestrator.execute(ORDER_ID));
        verify(steps).cancelOrder(eq(ORDER_ID), anyString());
    }
}
