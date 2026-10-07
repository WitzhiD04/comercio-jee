package com.comercio.query.application;

import com.comercio.query.domain.OrderSummaryView;
import com.comercio.query.infrastructure.ReadModelRepository;
import com.comercio.shared.event.OrderStatusChanged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.util.logging.Logger;

/**
 * Proyección CQRS de pedidos. Igual que {@link CatalogProjector}: AFTER_SUCCESS + REQUIRES_NEW.
 * Cada evento trae el resumen completo, así que hace upsert; la versión del pedido descarta eventos tardíos.
 */
@ApplicationScoped
public class OrderSummaryProjector {

    private static final Logger LOG = Logger.getLogger(OrderSummaryProjector.class.getName());

    @Inject
    ReadModelRepository views;

    @Transactional(TxType.REQUIRES_NEW)
    public void onOrderStatusChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderStatusChanged e) {
        OrderSummaryView view = views.findOrderForUpdate(e.orderId())
                .orElseGet(() -> views.persist(new OrderSummaryView(e.orderId())));
        if (view.getStatus() != null && e.version() <= view.getSourceVersion()) {
            LOG.fine(() -> "Evento de pedido obsoleto ignorado: " + e.orderId() + " " + e.status());
            return;
        }
        view.apply(e.customerId(), e.status(), e.total(), e.itemsCount(), e.occurredAt(), e.version());
        LOG.info(() -> "[CQRS] OrderSummaryView " + e.orderId() + " -> " + e.status());
    }
}
