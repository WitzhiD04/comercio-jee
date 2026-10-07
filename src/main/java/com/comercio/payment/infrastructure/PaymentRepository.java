package com.comercio.payment.infrastructure;

import com.comercio.payment.domain.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentRepository {

    @PersistenceContext(unitName = "paymentsPU")
    private EntityManager em;

    public Optional<Payment> findByOrderId(UUID orderId) {
        return em.createQuery("SELECT p FROM Payment p WHERE p.orderId = :orderId", Payment.class)
                .setParameter("orderId", orderId)
                .getResultStream()
                .findFirst();
    }

    public Payment save(Payment payment) {
        em.persist(payment);
        em.flush();
        return payment;
    }
}
