package com.comercio.catalog.application;

import com.comercio.catalog.domain.Product;
import com.comercio.catalog.infrastructure.ProductRepository;
import com.comercio.shared.exception.BusinessRuleException;
import com.comercio.shared.exception.ProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.math.BigDecimal;

/**
 * Precio vigente de un producto para fijarlo en la línea del pedido. Lee el modelo de escritura (y no la
 * vista CQRS) porque el precio cobrado no puede depender de una proyección con retraso.
 */
@ApplicationScoped
@Transactional(TxType.SUPPORTS)
public class ProductPricingService {

    @Inject
    ProductRepository products;

    public BigDecimal currentPrice(Long productId) {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.isActive()) {
            throw new BusinessRuleException("El producto " + productId + " no está activo");
        }
        return product.getPrice();
    }
}
