package com.comercio.catalog.application;

import com.comercio.catalog.domain.InventoryReservation;
import com.comercio.catalog.domain.Product;
import com.comercio.catalog.domain.ReservationStatus;
import com.comercio.catalog.infrastructure.InventoryReservationRepository;
import com.comercio.catalog.infrastructure.ProductRepository;
import com.comercio.shared.event.StockChanged;
import com.comercio.shared.exception.ProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Operaciones de inventario que usa la SAGA. Son {@code MANDATORY}: siempre se ejecutan dentro de la
 * transacción JTA del paso de la SAGA, que además escribe en ordersPU (pedido + bitácora). Así el cambio
 * en inventory_db y el registro en orders_db se confirman o se deshacen juntos (XA / 2PC).
 *
 * <p>Todas son idempotentes: reintentar un paso o una compensación no duplica efectos.
 */
@ApplicationScoped
@Transactional(TxType.MANDATORY)
public class InventoryCommandService {

    private static final Logger LOG = Logger.getLogger(InventoryCommandService.class.getName());

    @Inject
    ProductRepository products;

    @Inject
    InventoryReservationRepository reservations;

    @Inject
    Event<StockChanged> stockChanged;

    /**
     * Reserva todas las líneas o ninguna: si un producto no tiene stock se lanza
     * {@code InsufficientStockException} y la transacción completa hace rollback.
     * Los productos se bloquean (PESSIMISTIC_WRITE) en orden de id para evitar interbloqueos.
     */
    public void reserve(UUID orderId, List<ReservationLine> lines) {
        List<Product> touched = new ArrayList<>();
        lines.stream()
                .sorted(Comparator.comparing(ReservationLine::productId))
                .forEach(line -> {
                    if (reservations.find(orderId, line.productId()).isPresent()) {
                        LOG.info(() -> "Reserva ya existente (idempotencia) pedido=" + orderId
                                + " producto=" + line.productId());
                        return;
                    }
                    Product product = products.findByIdForUpdate(line.productId())
                            .orElseThrow(() -> new ProductNotFoundException(line.productId()));
                    product.reserve(line.quantity());
                    reservations.save(new InventoryReservation(orderId, line.productId(), line.quantity()));
                    touched.add(product);
                });
        publish(touched, "RESERVA");
    }

    /** Compensación: devuelve al disponible las unidades reservadas y deja las reservas en LIBERADA. */
    public int release(UUID orderId) {
        List<Product> touched = new ArrayList<>();
        for (InventoryReservation r : reservations.findByOrder(orderId)) {
            if (r.getStatus() != ReservationStatus.RESERVADA) {
                continue; // ya liberada/confirmada: nada que hacer (idempotencia)
            }
            Product product = lock(r.getProductId());
            product.release(r.getQuantity());
            r.changeStatus(ReservationStatus.LIBERADA);
            touched.add(product);
        }
        publish(touched, "LIBERACION");
        return touched.size();
    }

    /** Las reservas pasan a CONFIRMADA y las unidades salen del stock físico. */
    public int confirm(UUID orderId) {
        List<Product> touched = new ArrayList<>();
        for (InventoryReservation r : reservations.findByOrder(orderId)) {
            if (r.getStatus() != ReservationStatus.RESERVADA) {
                continue;
            }
            Product product = lock(r.getProductId());
            product.commitReservation(r.getQuantity());
            r.changeStatus(ReservationStatus.CONFIRMADA);
            touched.add(product);
        }
        publish(touched, "CONFIRMACION");
        return touched.size();
    }

    private Product lock(Long productId) {
        return products.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void publish(List<Product> touched, String cause) {
        if (touched.isEmpty()) {
            return;
        }
        products.flush(); // materializa el nuevo @Version antes de publicar
        Instant now = Instant.now();
        touched.forEach(p -> stockChanged.fire(
                new StockChanged(p.getId(), p.getStock(), p.getReserved(), p.getVersion(), cause, now)));
    }
}
