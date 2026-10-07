package com.comercio.order.infrastructure;

import com.comercio.order.domain.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderRepository {

    @PersistenceContext(unitName = "ordersPU")
    private EntityManager em;

    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }

    public Order save(Order order) {
        em.persist(order);
        return order;
    }

    public void flush() {
        em.flush();
    }
}
