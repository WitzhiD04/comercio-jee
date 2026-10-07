package com.comercio.catalog.application;

import com.comercio.catalog.domain.Product;
import com.comercio.catalog.infrastructure.ProductRepository;
import com.comercio.shared.event.ProductChanged;
import com.comercio.shared.event.StockChanged;
import com.comercio.shared.exception.DuplicateSkuException;
import com.comercio.shared.exception.ProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Comandos del catálogo (lado de escritura de CQRS). Cada método es una transacción JTA sobre inventoryPU
 * y publica {@link ProductChanged}/{@link StockChanged}; las proyecciones los aplican después del commit.
 */
@ApplicationScoped
@Transactional
public class ProductCommandService {

    private static final Logger LOG = Logger.getLogger(ProductCommandService.class.getName());

    @Inject
    ProductRepository products;

    @Inject
    Event<ProductChanged> productChanged;

    @Inject
    Event<StockChanged> stockChanged;

    public Product create(String sku, String name, BigDecimal price, int stock, boolean active) {
        if (products.findBySku(sku).isPresent()) {
            throw new DuplicateSkuException(sku);
        }
        Product product = products.save(new Product(sku, name, price, stock, active));
        products.flush();
        LOG.info(() -> "Producto creado id=" + product.getId() + " sku=" + sku);
        publishProductChanged(product);
        return product;
    }

    public Product update(Long id, String sku, String name, BigDecimal price, int stock, boolean active) {
        Product product = find(id);
        if (!product.getSku().equals(sku) && products.findBySku(sku).isPresent()) {
            throw new DuplicateSkuException(sku);
        }
        product.update(sku, name, price, stock, active);
        products.flush();
        publishProductChanged(product);
        return product;
    }

    public Product changePrice(Long id, BigDecimal price) {
        Product product = find(id);
        product.setPrice(price);
        products.flush();
        publishProductChanged(product);
        return product;
    }

    /** Ajuste de existencias y/o activación. Los parámetros nulos no se modifican. */
    public Product changeAvailability(Long id, Integer stock, Boolean active) {
        Product product = find(id);
        if (stock != null) {
            product.changeStock(stock);
        }
        if (active != null) {
            product.setActive(active);
        }
        products.flush();
        if (active != null) {
            publishProductChanged(product);
        } else {
            stockChanged.fire(new StockChanged(product.getId(), product.getStock(), product.getReserved(),
                    product.getVersion(), "AJUSTE_MANUAL", Instant.now()));
        }
        return product;
    }

    /** Borrado lógico: el producto deja de poder venderse, pero se conserva para el histórico de pedidos. */
    public Product deactivate(Long id) {
        return changeAvailability(id, null, false);
    }

    /** Publica la foto actual de todos los productos (reconstrucción del modelo de lectura). */
    public int republishCatalog() {
        var all = products.findAll();
        all.forEach(this::publishProductChanged);
        LOG.info(() -> "Catálogo republicado para las proyecciones: " + all.size() + " productos");
        return all.size();
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void publishProductChanged(Product p) {
        productChanged.fire(new ProductChanged(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(),
                p.getReserved(), p.isActive(), p.getVersion(), Instant.now()));
    }
}
