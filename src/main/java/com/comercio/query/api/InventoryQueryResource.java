package com.comercio.query.api;

import com.comercio.query.api.QueryDtos.InventoryItem;
import com.comercio.query.application.CatalogQueryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Inventario con disponible = stock − reservado (queryPU). */
@Path("inventory")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class InventoryQueryResource {

    @Inject
    CatalogQueryService queries;

    @GET
    public List<InventoryItem> list() {
        return queries.listProducts().stream().map(InventoryItem::from).toList();
    }
}
