package com.comercio.query.api;

import com.comercio.query.api.QueryDtos.ProductView;
import com.comercio.query.application.CatalogQueryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Consultas de productos (lado de lectura de CQRS, queryPU). Comparte la ruta /products con ProductCommandResource. */
@Path("products")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ProductQueryResource {

    @Inject
    CatalogQueryService queries;

    @GET
    public List<ProductView> list() {
        return queries.listProducts().stream().map(ProductView::from).toList();
    }

    @GET
    @Path("{id}")
    public ProductView get(@PathParam("id") Long id) {
        return ProductView.from(queries.getProduct(id));
    }
}
