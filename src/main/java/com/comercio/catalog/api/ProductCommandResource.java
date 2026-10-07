package com.comercio.catalog.api;

import com.comercio.catalog.api.ProductDtos.AvailabilityChangeRequest;
import com.comercio.catalog.api.ProductDtos.CreateProductRequest;
import com.comercio.catalog.api.ProductDtos.PriceChangeRequest;
import com.comercio.catalog.api.ProductDtos.ProductResponse;
import com.comercio.catalog.api.ProductDtos.UpdateProductRequest;
import com.comercio.catalog.application.ProductCommandService;
import com.comercio.catalog.domain.Product;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * API de comandos del catálogo (escritura). Las lecturas de /products están en
 * {@code com.comercio.query.api.ProductQueryResource} y se sirven desde el modelo de lectura.
 */
@Path("products")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductCommandResource {

    @Inject
    ProductCommandService commands;

    @POST
    public Response create(@Valid @NotNull CreateProductRequest request, @Context UriInfo uriInfo) {
        Product p = commands.create(request.sku(), request.name(), request.price(), request.stock(),
                request.active() == null || request.active());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(p.getId())).build())
                .entity(ProductResponse.from(p))
                .build();
    }

    @PUT
    @Path("{id}")
    public ProductResponse update(@PathParam("id") Long id, @Valid @NotNull UpdateProductRequest request) {
        return ProductResponse.from(commands.update(id, request.sku(), request.name(), request.price(),
                request.stock(), request.active()));
    }

    @PATCH
    @Path("{id}/price")
    public ProductResponse changePrice(@PathParam("id") Long id, @Valid @NotNull PriceChangeRequest request) {
        return ProductResponse.from(commands.changePrice(id, request.price()));
    }

    @PATCH
    @Path("{id}/availability")
    public ProductResponse changeAvailability(@PathParam("id") Long id,
                                              @Valid @NotNull AvailabilityChangeRequest request) {
        return ProductResponse.from(commands.changeAvailability(id, request.stock(), request.active()));
    }

    /** Borrado lógico (active=false). */
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        commands.deactivate(id);
        return Response.noContent().build();
    }
}
