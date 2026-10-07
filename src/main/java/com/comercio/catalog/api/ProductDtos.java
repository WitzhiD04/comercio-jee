package com.comercio.catalog.api;

import com.comercio.catalog.domain.Product;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** DTOs (records) de la API de comandos del catálogo. Las entidades JPA nunca salen de la capa de aplicación. */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** POST /products. {@code active} es opcional (por defecto true). */
    public record CreateProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 200) String name,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stock,
            Boolean active) {
    }

    /** PUT /products/{id}: reemplazo completo. */
    public record UpdateProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 200) String name,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stock,
            @NotNull Boolean active) {
    }

    /** PATCH /products/{id}/price */
    public record PriceChangeRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal price) {
    }

    /** PATCH /products/{id}/availability: stock y/o active. */
    public record AvailabilityChangeRequest(@Min(0) Integer stock, Boolean active) {

        @AssertTrue(message = "debe indicar stock y/o active")
        public boolean isAnyFieldPresent() {
            return stock != null || active != null;
        }
    }

    /** Respuesta de los comandos: estado del modelo de escritura justo después del commit. */
    public record ProductResponse(Long id, String sku, String name, BigDecimal price, int stock, int reserved,
                                  int available, boolean active, long version) {

        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(),
                    p.getReserved(), p.available(), p.isActive(), p.getVersion());
        }
    }
}
