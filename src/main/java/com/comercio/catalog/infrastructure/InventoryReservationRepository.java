package com.comercio.catalog.infrastructure;

import com.comercio.catalog.domain.InventoryReservation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InventoryReservationRepository {

    @PersistenceContext(unitName = "inventoryPU")
    private EntityManager em;

    public Optional<InventoryReservation> find(UUID orderId, Long productId) {
        return em.createQuery("SELECT r FROM InventoryReservation r "
                        + "WHERE r.orderId = :orderId AND r.productId = :productId", InventoryReservation.class)
                .setParameter("orderId", orderId)
                .setParameter("productId", productId)
                .getResultStream()
                .findFirst();
    }

    public List<InventoryReservation> findByOrder(UUID orderId) {
        return em.createQuery("SELECT r FROM InventoryReservation r WHERE r.orderId = :orderId "
                        + "ORDER BY r.productId", InventoryReservation.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    public InventoryReservation save(InventoryReservation reservation) {
        em.persist(reservation);
        return reservation;
    }
}
