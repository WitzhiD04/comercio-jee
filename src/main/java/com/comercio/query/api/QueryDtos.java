package com.comercio.query.api;

import com.comercio.query.domain.OrderSummaryView;
import com.comercio.query.domain.PaymentView;
import com.comercio.query.domain.ProductCatalogView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTOs (records) de la API de consultas, construidos a partir de las vistas de queryPU. */
public final class QueryDtos {

    private QueryDtos() {
    }

    public record ProductView(Long productId, String sku, String name, BigDecimal price, int stock, int reserved,
                              int available, boolean active, Instant updatedAt) {

        static ProductView from(ProductCatalogView v) {
            return new ProductView(v.getProductId(), v.getSku(), v.getName(), v.getPrice(), v.getStock(),
                    v.getReserved(), v.getAvailable(), v.isActive(), v.getUpdatedAt());
        }
    }

    /** GET /inventory: disponible = stock − reservado. */
    public record InventoryItem(Long productId, String sku, String name, int stock, int reserved, int available,
                                boolean active) {

        static InventoryItem from(ProductCatalogView v) {
            return new InventoryItem(v.getProductId(), v.getSku(), v.getName(), v.getStock(), v.getReserved(),
                    v.getAvailable(), v.isActive());
        }
    }

    public record OrderSummary(UUID orderId, String customerId, String status, BigDecimal total, int itemsCount,
                               Instant updatedAt) {

        static OrderSummary from(OrderSummaryView v) {
            return new OrderSummary(v.getOrderId(), v.getCustomerId(), v.getStatus(), v.getTotal(),
                    v.getItemsCount(), v.getUpdatedAt());
        }
    }

    public record PaymentSummary(Long paymentId, UUID orderId, BigDecimal amount, String status, String reason,
                                 Instant processedAt) {

        static PaymentSummary from(PaymentView v) {
            return new PaymentSummary(v.getPaymentId(), v.getOrderId(), v.getAmount(), v.getStatus(),
                    v.getReason(), v.getProcessedAt());
        }
    }
}
