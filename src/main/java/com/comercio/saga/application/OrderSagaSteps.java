package com.comercio.saga.application;

import com.comercio.catalog.application.InventoryCommandService;
import com.comercio.catalog.application.ReservationLine;
import com.comercio.order.application.OrderCommandService;
import com.comercio.order.domain.Order;
import com.comercio.order.domain.OrderStatus;
import com.comercio.payment.application.PaymentCommandService;
import com.comercio.payment.domain.Payment;
import com.comercio.saga.domain.SagaInstance;
import com.comercio.saga.domain.SagaStatus;
import com.comercio.saga.domain.StepAction;
import com.comercio.saga.domain.StepResult;
import com.comercio.saga.infrastructure.SagaInstanceRepository;
import com.comercio.shared.exception.InvalidOrderStateException;
import com.comercio.shared.exception.PaymentRejectedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Pasos y compensaciones de la SAGA de pedidos. Cada método público es UNA transacción local de la SAGA.
 *
 * <h2>JTA / XA dentro de cada paso</h2>
 * Todos los métodos son {@code @Transactional(REQUIRES_NEW)}: el interceptor de JTA suspende cualquier
 * transacción previa y abre una nueva solo para el paso. Dentro de ella se escribe en DOS bases:
 * <ul>
 *   <li>el cambio de dominio: {@code inventory_db} (inventoryPU) o {@code payments_db} (paymentsPU), y</li>
 *   <li>el cambio de estado del pedido + la entrada {@code SagaStep} de la bitácora: {@code orders_db} (ordersPU).</li>
 * </ul>
 * Las dos unidades usan datasources XA ({@code PGXADataSource}), así que el gestor de transacciones de
 * Payara las enlista en la MISMA transacción JTA distribuida y al salir del método ejecuta two-phase commit:
 * {@code PREPARE TRANSACTION} en ambas bases y, solo si las dos responden OK, {@code COMMIT PREPARED}.
 * Si cualquier escritura falla (sin stock, error SQL, caída de una base durante el prepare...) se hace
 * rollback de AMBAS: nunca queda, por ejemplo, stock reservado sin que el pedido y la bitácora lo reflejen.
 *
 * <p>Los servicios de dominio que se invocan aquí ({@code InventoryCommandService},
 * {@code PaymentCommandService}, {@code OrderCommandService#changeStatus}) son {@code MANDATORY}:
 * no pueden ejecutarse fuera de la transacción del paso.
 */
@ApplicationScoped
public class OrderSagaSteps {

    public static final String INICIAR_SAGA = "INICIAR_SAGA";
    public static final String RESERVAR_INVENTARIO = "RESERVAR_INVENTARIO";
    public static final String PROCESAR_PAGO = "PROCESAR_PAGO";
    public static final String CONFIRMAR_PEDIDO = "CONFIRMAR_PEDIDO";
    public static final String LIBERAR_INVENTARIO = "LIBERAR_INVENTARIO";
    public static final String CANCELAR_PEDIDO = "CANCELAR_PEDIDO";

    private static final Logger LOG = Logger.getLogger(OrderSagaSteps.class.getName());

    @Inject
    OrderCommandService orders;

    @Inject
    InventoryCommandService inventory;

    @Inject
    PaymentCommandService payments;

    @Inject
    SagaInstanceRepository sagas;

    /** Crea (idempotente) la instancia de la SAGA en INICIADA. Solo toca orders_db. */
    @Transactional(TxType.REQUIRES_NEW)
    public SagaOrderData start(UUID orderId) {
        Order order = orders.get(orderId);
        if (sagas.findByOrderId(orderId).isEmpty()) {
            SagaInstance saga = sagas.save(new SagaInstance(orderId));
            saga.record(INICIAR_SAGA, StepAction.EJECUTAR, StepResult.OK,
                    "Pedido en " + order.getStatus() + ", total " + order.getTotal().toPlainString());
        }
        LOG.info(() -> "[SAGA " + orderId + "] INICIADA");
        return SagaOrderData.from(order);
    }

    /**
     * Paso 1. Transacción XA: inventory_db (reservas + productos) y orders_db (pedido + bitácora).
     * Si no hay stock, {@code InsufficientStockException} provoca rollback de las dos bases.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void reserveInventory(UUID orderId, List<ReservationLine> items) {
        LOG.info(() -> "[SAGA " + orderId + "] Paso 1: reservar inventario " + items);
        inventory.reserve(orderId, items);                                  // inventory_db
        orders.changeStatus(orderId, OrderStatus.INVENTARIO_RESERVADO);    // orders_db
        saga(orderId).record(RESERVAR_INVENTARIO, StepAction.EJECUTAR, StepResult.OK,
                items.size() + " producto(s) reservado(s)");                // orders_db
        // commit en dos fases (inventory_db + orders_db) al salir del método
    }

    /**
     * Paso 2. Transacción XA: payments_db (pago) y orders_db (pedido + bitácora).
     *
     * <p>Un rechazo de la pasarela NO es un error técnico: el pago RECHAZADO, el pedido en PAGO_RECHAZADO y
     * el paso con resultado ERROR deben quedar confirmados juntos. Por eso {@code dontRollbackOn}: se hace
     * commit 2PC y después se propaga {@link PaymentRejectedException} para que el orquestador compense.
     */
    @Transactional(value = TxType.REQUIRES_NEW, dontRollbackOn = PaymentRejectedException.class)
    public void processPayment(UUID orderId, BigDecimal amount) {
        LOG.info(() -> "[SAGA " + orderId + "] Paso 2: procesar pago de " + amount.toPlainString());
        Order order = orders.get(orderId);
        Payment payment = payments.process(orderId, amount, order.getPaymentMethod());   // payments_db
        SagaInstance saga = saga(orderId);
        if (payment.isApproved()) {
            orders.changeStatus(orderId, OrderStatus.PAGO_APROBADO);                     // orders_db
            saga.record(PROCESAR_PAGO, StepAction.EJECUTAR, StepResult.OK,
                    "Pago " + payment.getId() + " aprobado");                             // orders_db
            return;
        }
        orders.changeStatus(orderId, OrderStatus.PAGO_RECHAZADO);                         // orders_db
        saga.record(PROCESAR_PAGO, StepAction.EJECUTAR, StepResult.ERROR, payment.getReason());
        saga.changeStatus(SagaStatus.COMPENSANDO);
        LOG.warning(() -> "[SAGA " + orderId + "] Pago rechazado: " + payment.getReason());
        throw new PaymentRejectedException(orderId, payment.getReason());               // commit + aviso
    }

    /** Paso 3. Transacción XA: inventory_db (reservas a CONFIRMADA, salida de stock) y orders_db. */
    @Transactional(TxType.REQUIRES_NEW)
    public void confirmOrder(UUID orderId) {
        LOG.info(() -> "[SAGA " + orderId + "] Paso 3: confirmar pedido");
        int confirmed = inventory.confirm(orderId);                          // inventory_db
        orders.changeStatus(orderId, OrderStatus.CONFIRMADO);               // orders_db
        SagaInstance saga = saga(orderId);
        saga.record(CONFIRMAR_PEDIDO, StepAction.EJECUTAR, StepResult.OK,
                confirmed + " reserva(s) confirmada(s)");
        saga.changeStatus(SagaStatus.COMPLETADA);
        LOG.info(() -> "[SAGA " + orderId + "] COMPLETADA");
    }

    /**
     * Compensación del paso 1. Transacción XA: inventory_db (reservas a LIBERADA, unidades de vuelta al
     * disponible) y orders_db (PAGO_RECHAZADO → INVENTARIO_LIBERADO + bitácora).
     * Idempotente: si las reservas ya estaban liberadas o el pedido ya avanzó, no repite el efecto.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void releaseInventory(UUID orderId) {
        LOG.info(() -> "[SAGA " + orderId + "] Compensación: liberar inventario");
        int released = inventory.release(orderId);                          // inventory_db
        Order order = orders.get(orderId);
        if (order.getStatus() == OrderStatus.PAGO_RECHAZADO) {
            orders.changeStatus(orderId, OrderStatus.INVENTARIO_LIBERADO);  // orders_db
        } else if (order.getStatus() != OrderStatus.INVENTARIO_LIBERADO) {
            throw new InvalidOrderStateException("No se puede liberar inventario del pedido " + orderId
                    + " en estado " + order.getStatus());
        }
        SagaInstance saga = saga(orderId);
        saga.changeStatus(SagaStatus.COMPENSANDO);
        saga.record(LIBERAR_INVENTARIO, StepAction.COMPENSAR, StepResult.OK,
                released + " reserva(s) liberada(s)");
    }

    /**
     * Compensación del registro del pedido (la última en ejecutarse). Solo orders_db: pedido a CANCELADO y
     * SAGA a COMPENSADA. Idempotente: un pedido ya cancelado no se vuelve a transicionar.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void cancelOrder(UUID orderId, String reason) {
        LOG.info(() -> "[SAGA " + orderId + "] Compensación: cancelar pedido (" + reason + ")");
        if (orders.get(orderId).getStatus() != OrderStatus.CANCELADO) {
            orders.changeStatus(orderId, OrderStatus.CANCELADO);
        }
        SagaInstance saga = saga(orderId);
        saga.record(CANCELAR_PEDIDO, StepAction.COMPENSAR, StepResult.OK, reason);
        saga.changeStatus(SagaStatus.COMPENSADA);
        LOG.info(() -> "[SAGA " + orderId + "] COMPENSADA");
    }

    /**
     * Fallo técnico del pago (la transacción del paso 2 hizo rollback y no hay pago registrado):
     * se deja el pedido en PAGO_RECHAZADO para poder compensar.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void markPaymentFailed(UUID orderId, String detail) {
        if (orders.get(orderId).getStatus() == OrderStatus.INVENTARIO_RESERVADO) {
            orders.changeStatus(orderId, OrderStatus.PAGO_RECHAZADO);
        }
        SagaInstance saga = saga(orderId);
        saga.record(PROCESAR_PAGO, StepAction.EJECUTAR, StepResult.ERROR, detail);
        saga.changeStatus(SagaStatus.COMPENSANDO);
    }

    /** Registra en la bitácora un intento fallido (la transacción del paso ya hizo rollback). */
    @Transactional(TxType.REQUIRES_NEW)
    public void recordStepFailure(UUID orderId, String stepName, StepAction action, String detail) {
        saga(orderId).record(stepName, action, StepResult.ERROR, detail);
    }

    /** Reintentos agotados: la SAGA queda FALLIDA y requiere intervención manual. */
    @Transactional(TxType.REQUIRES_NEW)
    public void markFailed(UUID orderId, String stepName, StepAction action, String detail) {
        SagaInstance saga = saga(orderId);
        saga.record(stepName, action, StepResult.ERROR, detail);
        saga.changeStatus(SagaStatus.FALLIDA);
        LOG.severe(() -> "[SAGA " + orderId + "] FALLIDA en " + stepName + ": " + detail);
    }

    private SagaInstance saga(UUID orderId) {
        return sagas.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No existe SAGA para el pedido " + orderId));
    }
}
