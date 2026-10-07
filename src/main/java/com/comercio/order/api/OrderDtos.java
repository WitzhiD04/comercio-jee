package com.comercio.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** DTOs (records) de la API de comandos de pedidos. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** POST /orders: {@code {customerId, items:[{productId, quantity}], paymentMethod}}. */
    public record PlaceOrderRequest(
            @NotBlank @Size(max = 64) String customerId,
            @NotEmpty @Size(max = 50) List<@Valid @NotNull OrderItemRequest> items,
            @NotBlank @Size(max = 32) String paymentMethod) {
    }

    public record OrderItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }

    /** Respuesta 202: el pedido quedó PENDIENTE y la SAGA corre en segundo plano. */
    public record OrderAcceptedResponse(UUID orderId, String status) {
    }
}
