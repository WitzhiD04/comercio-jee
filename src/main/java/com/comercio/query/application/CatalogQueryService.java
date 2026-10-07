package com.comercio.query.application;

import com.comercio.query.domain.ProductCatalogView;
import com.comercio.query.infrastructure.ReadModelRepository;
import com.comercio.shared.exception.ProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/** Consultas de catálogo e inventario. Solo leen queryPU. */
@ApplicationScoped
public class CatalogQueryService {

    @Inject
    ReadModelRepository views;

    public List<ProductCatalogView> listProducts() {
        return views.findAllProducts();
    }

    public ProductCatalogView getProduct(Long productId) {
        return views.findProduct(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
