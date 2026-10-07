package com.comercio.query.infrastructure;

import com.comercio.query.domain.OrderSummaryView;
import com.comercio.query.domain.PaymentView;
import com.comercio.query.domain.ProductCatalogView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Acceso al modelo de lectura (queryPU). Las consultas de la API solo pasan por aquí. */
@ApplicationScoped
public class ReadModelRepository {

    @PersistenceContext(unitName = "queryPU")
    private EntityManager em;

    // ---- Catálogo / inventario ----

    public List<ProductCatalogView> findAllProducts() {
        return em.createQuery("SELECT v FROM ProductCatalogView v ORDER BY v.productId", ProductCatalogView.class)
                .getResultList();
    }

    public Optional<ProductCatalogView> findProduct(Long productId) {
        return Optional.ofNullable(em.find(ProductCatalogView.class, productId));
    }

    /** Bloquea la fila de la vista para que dos proyecciones concurrentes del mismo producto se serialicen. */
    public Optional<ProductCatalogView> findProductForUpdate(Long productId) {
        return Optional.ofNullable(em.find(ProductCatalogView.class, productId, LockModeType.PESSIMISTIC_WRITE));
    }

    // ---- Pedidos ----

    public Optional<OrderSummaryView> findOrder(UUID orderId) {
        return Optional.ofNullable(em.find(OrderSummaryView.class, orderId));
    }

    public Optional<OrderSummaryView> findOrderForUpdate(UUID orderId) {
        return Optional.ofNullable(em.find(OrderSummaryView.class, orderId, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<OrderSummaryView> findOrdersByCustomer(String customerId) {
        return em.createQuery("SELECT v FROM OrderSummaryView v WHERE v.customerId = :customerId "
                        + "ORDER BY v.updatedAt DESC", OrderSummaryView.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<OrderSummaryView> findRecentOrders(int max) {
        return em.createQuery("SELECT v FROM OrderSummaryView v ORDER BY v.updatedAt DESC", OrderSummaryView.class)
                .setMaxResults(max)
                .getResultList();
    }

    // ---- Pagos ----

    public List<PaymentView> findPaymentsByOrder(UUID orderId) {
        return em.createQuery("SELECT v FROM PaymentView v WHERE v.orderId = :orderId ORDER BY v.processedAt",
                        PaymentView.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    public List<PaymentView> findRecentPayments(int max) {
        return em.createQuery("SELECT v FROM PaymentView v ORDER BY v.processedAt DESC", PaymentView.class)
                .setMaxResults(max)
                .getResultList();
    }

    public Optional<PaymentView> findPayment(Long paymentId) {
        return Optional.ofNullable(em.find(PaymentView.class, paymentId));
    }

    public <T> T persist(T view) {
        em.persist(view);
        return view;
    }
}
