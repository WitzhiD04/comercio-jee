package com.comercio.saga.infrastructure;

import com.comercio.saga.domain.SagaInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SagaInstanceRepository {

    @PersistenceContext(unitName = "ordersPU")
    private EntityManager em;

    public Optional<SagaInstance> findByOrderId(UUID orderId) {
        return em.createQuery("SELECT s FROM SagaInstance s WHERE s.orderId = :orderId", SagaInstance.class)
                .setParameter("orderId", orderId)
                .getResultStream()
                .findFirst();
    }

    public SagaInstance save(SagaInstance saga) {
        em.persist(saga);
        return saga;
    }
}
