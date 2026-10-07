package com.comercio.query.application;

import com.comercio.query.domain.ProductCatalogView;
import com.comercio.query.infrastructure.ReadModelRepository;
import com.comercio.shared.event.ProductChanged;
import com.comercio.shared.event.StockChanged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.util.logging.Logger;

/**
 * Proyección CQRS del catálogo: mantiene {@link ProductCatalogView} a partir de los eventos del modelo de
 * escritura.
 *
 * <p>{@code during = AFTER_SUCCESS}: el observador solo se ejecuta si la transacción que publicó el evento
 * hizo commit (nunca se proyecta un cambio que luego se deshizo). Corre en una transacción NUEVA
 * ({@code REQUIRES_NEW}) sobre queryPU: la vista queda al día un instante después del commit del comando,
 * es decir, consistencia eventual intencional. Si la proyección falla, el comando ya está confirmado.
 */
@ApplicationScoped
public class CatalogProjector {

    private static final Logger LOG = Logger.getLogger(CatalogProjector.class.getName());

    @Inject
    ReadModelRepository views;

    @Transactional(TxType.REQUIRES_NEW)
    public void onProductChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) ProductChanged e) {
        ProductCatalogView view = views.findProductForUpdate(e.productId())
                .orElseGet(() -> views.persist(new ProductCatalogView(e.productId())));
        if (view.getUpdatedAt() != null && e.version() < view.getSourceVersion()) {
            LOG.fine(() -> "Evento obsoleto ignorado para producto " + e.productId());
            return;
        }
        view.applyProduct(e.sku(), e.name(), e.price(), e.active());
        view.applyStock(e.stock(), e.reserved());
        view.markVersion(e.version(), e.occurredAt());
        LOG.info(() -> "[CQRS] ProductCatalogView " + e.productId() + " v" + e.version() + " actualizada");
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void onStockChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StockChanged e) {
        var maybeView = views.findProductForUpdate(e.productId());
        if (maybeView.isEmpty()) {
            LOG.warning(() -> "[CQRS] StockChanged para un producto sin vista: " + e.productId());
            return;
        }
        ProductCatalogView view = maybeView.get();
        if (e.version() <= view.getSourceVersion()) {
            LOG.fine(() -> "Evento de stock obsoleto ignorado para producto " + e.productId());
            return;
        }
        view.applyStock(e.stock(), e.reserved());
        view.markVersion(e.version(), e.occurredAt());
        LOG.info(() -> "[CQRS] Stock de " + e.productId() + " (" + e.cause() + "): stock=" + e.stock()
                + " reservado=" + e.reserved() + " disponible=" + view.getAvailable());
    }
}
