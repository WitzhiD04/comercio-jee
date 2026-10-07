package com.comercio.saga.application;

import com.comercio.saga.domain.SagaStatus;
import com.comercio.saga.domain.StepAction;
import com.comercio.shared.event.OrderPlaced;
import com.comercio.shared.exception.DomainException;
import com.comercio.shared.exception.PaymentRejectedException;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.comercio.saga.application.OrderSagaSteps.CANCELAR_PEDIDO;
import static com.comercio.saga.application.OrderSagaSteps.CONFIRMAR_PEDIDO;
import static com.comercio.saga.application.OrderSagaSteps.LIBERAR_INVENTARIO;
import static com.comercio.saga.application.OrderSagaSteps.PROCESAR_PAGO;
import static com.comercio.saga.application.OrderSagaSteps.RESERVAR_INVENTARIO;

/**
 * Orquestador de la SAGA de pedidos: decide qué paso sigue y, ante un fallo, ejecuta las compensaciones
 * en orden inverso. No abre transacciones: cada paso de {@link OrderSagaSteps} confirma la suya
 * (REQUIRES_NEW), así que no hay una transacción global que abarque toda la SAGA.
 *
 * <pre>
 *   T1 registrar pedido (POST /orders)      C1 cancelar pedido      ─┐
 *   T2 reservar inventario                  C2 liberar inventario    │ compensables
 *   T3 procesar pago        ← pivote: a partir de aquí no se compensa ┘
 *   T4 confirmar pedido     ← reintentable: si se agotan los reintentos la SAGA queda FALLIDA
 * </pre>
 * Las compensaciones se apilan a medida que los pasos se completan y se desapilan (LIFO) al fallar.
 */
@ApplicationScoped
public class OrderSagaOrchestrator {

    /** Reintentos adicionales tras el primer intento (compensaciones y fallos técnicos de los pasos). */
    static final int MAX_RETRIES = 3;

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class.getName());

    private OrderSagaSteps steps;

    private long retryBackoffMillis = 200;

    @Resource
    ManagedExecutorService executor;

    /** Requerido por CDI para crear el proxy del bean. */
    protected OrderSagaOrchestrator() {
    }

    @Inject
    public OrderSagaOrchestrator(OrderSagaSteps steps) {
        this.steps = steps;
    }

    /** Para pruebas: sin espera entre reintentos. */
    OrderSagaOrchestrator(OrderSagaSteps steps, long retryBackoffMillis) {
        this.steps = steps;
        this.retryBackoffMillis = retryBackoffMillis;
    }

    /**
     * Arranque de la SAGA. {@code AFTER_SUCCESS}: solo se ejecuta si la transacción de POST /orders hizo
     * commit, es decir, si el pedido quedó realmente persistido. La SAGA corre en un hilo del
     * {@link ManagedExecutorService} para que la petición HTTP responda 202 sin esperarla.
     */
    void onOrderPlaced(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderPlaced event) {
        UUID orderId = event.orderId();
        executor.submit(() -> {
            try {
                execute(orderId);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "[SAGA " + orderId + "] Error inesperado en el orquestador", e);
            }
        });
    }

    /** Ejecuta la SAGA completa de forma síncrona y devuelve su estado final. */
    public SagaStatus execute(UUID orderId) {
        SagaOrderData order = steps.start(orderId);
        Deque<Compensation> compensations = new ArrayDeque<>();
        compensations.push(new Compensation(CANCELAR_PEDIDO,
                () -> steps.cancelOrder(orderId, "Pedido cancelado por la SAGA")));

        // T2: reservar inventario
        try {
            runStep(orderId, RESERVAR_INVENTARIO, StepAction.EJECUTAR, false,
                    () -> steps.reserveInventory(orderId, order.items()));
        } catch (RuntimeException e) {
            // La transacción XA del paso hizo rollback: no se reservó nada, solo queda cancelar el pedido
            LOG.warning(() -> "[SAGA " + orderId + "] No se pudo reservar inventario: " + reason(e));
            return compensate(orderId, compensations);
        }
        compensations.push(new Compensation(LIBERAR_INVENTARIO, () -> steps.releaseInventory(orderId)));

        // T3: procesar pago (pivote)
        try {
            runStep(orderId, PROCESAR_PAGO, StepAction.EJECUTAR, false,
                    () -> steps.processPayment(orderId, order.total()));
        } catch (PaymentRejectedException e) {
            // Ya confirmado en la misma transacción del paso: pago RECHAZADO + pedido PAGO_RECHAZADO
            return compensate(orderId, compensations);
        } catch (RuntimeException e) {
            safely(orderId, () -> steps.markPaymentFailed(orderId, "Pago no procesado: " + reason(e)));
            return compensate(orderId, compensations);
        }

        // T4: confirmar (después del pivote ya no se compensa: se reintenta)
        try {
            runStep(orderId, CONFIRMAR_PEDIDO, StepAction.EJECUTAR, true,
                    () -> steps.confirmOrder(orderId));
        } catch (RuntimeException e) {
            safely(orderId, () -> steps.markFailed(orderId, CONFIRMAR_PEDIDO, StepAction.EJECUTAR,
                    "Reintentos agotados: " + reason(e)));
            return SagaStatus.FALLIDA;
        }
        LOG.info(() -> "[SAGA " + orderId + "] Finalizada: COMPLETADA");
        return SagaStatus.COMPLETADA;
    }

    /** Desapila y ejecuta las compensaciones (orden inverso a los pasos), cada una con reintentos. */
    private SagaStatus compensate(UUID orderId, Deque<Compensation> compensations) {
        LOG.warning(() -> "[SAGA " + orderId + "] Iniciando compensaciones: " + compensations.stream()
                .map(Compensation::name).toList());
        while (!compensations.isEmpty()) {
            Compensation compensation = compensations.pop();
            try {
                runStep(orderId, compensation.name(), StepAction.COMPENSAR, true, compensation.action());
                LOG.info(() -> "[SAGA " + orderId + "] Compensación " + compensation.name() + " OK");
            } catch (RuntimeException e) {
                safely(orderId, () -> steps.markFailed(orderId, compensation.name(), StepAction.COMPENSAR,
                        "Compensación sin éxito tras " + (MAX_RETRIES + 1) + " intentos: " + reason(e)));
                return SagaStatus.FALLIDA;
            }
        }
        LOG.info(() -> "[SAGA " + orderId + "] Finalizada: COMPENSADA");
        return SagaStatus.COMPENSADA;
    }

    /**
     * Ejecuta un paso; cada intento fallido queda en la bitácora. Los fallos técnicos se reintentan hasta
     * {@link #MAX_RETRIES} veces; los de negocio (sin stock, pago rechazado...) solo si
     * {@code retryBusinessErrors} (compensaciones y paso final), porque repetirlos no cambia el resultado.
     */
    private void runStep(UUID orderId, String stepName, StepAction action, boolean retryBusinessErrors,
                         Runnable body) {
        for (int attempt = 1; ; attempt++) {
            try {
                body.run();
                return;
            } catch (RuntimeException e) {
                boolean business = isBusinessFailure(e);
                if (!(e instanceof PaymentRejectedException)) { // el rechazo ya quedó registrado en su paso
                    int n = attempt;
                    safely(orderId, () -> steps.recordStepFailure(orderId, stepName, action,
                            "Intento " + n + ": " + reason(e)));
                }
                if ((business && !retryBusinessErrors) || attempt > MAX_RETRIES) {
                    throw e;
                }
                int n = attempt;
                LOG.warning(() -> "[SAGA " + orderId + "] " + stepName + " falló (intento " + n + "), reintentando: "
                        + reason(e));
                pause(attempt);
            }
        }
    }

    private void pause(int attempt) {
        if (retryBackoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMillis * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Registrar en la bitácora no debe interrumpir la SAGA: si falla, se deja en el log del servidor. */
    private void safely(UUID orderId, Runnable bookkeeping) {
        try {
            bookkeeping.run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "[SAGA " + orderId + "] No se pudo actualizar la bitácora", e);
        }
    }

    private static boolean isBusinessFailure(Throwable e) {
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof DomainException) {
                return true;
            }
        }
        return false;
    }

    private static String reason(Throwable e) {
        Throwable root = e;
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            root = t;
            if (t instanceof DomainException) {
                break;
            }
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    private record Compensation(String name, Runnable action) {
    }
}
