package com.comercio.catalog.infrastructure;

import com.comercio.catalog.domain.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository {

    @PersistenceContext(unitName = "inventoryPU")
    private EntityManager em;

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    /**
     * SELECT ... FOR UPDATE: serializa las reservas concurrentes sobre el mismo producto hasta el
     * commit de la transacción JTA, evitando sobreventa sin reintentos.
     */
    public Optional<Product> findByIdForUpdate(Long id) {
        return Optional.ofNullable(em.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public Optional<Product> findBySku(String sku) {
        return em.createQuery("SELECT p FROM Product p WHERE p.sku = :sku", Product.class)
                .setParameter("sku", sku)
                .getResultStream()
                .findFirst();
    }

    public List<Product> findAll() {
        return em.createQuery("SELECT p FROM Product p ORDER BY p.id", Product.class).getResultList();
    }

    public Product save(Product product) {
        em.persist(product);
        return product;
    }

    /** Fuerza el SQL pendiente para que errores y el nuevo {@code @Version} aparezcan dentro del método. */
    public void flush() {
        em.flush();
    }
}
