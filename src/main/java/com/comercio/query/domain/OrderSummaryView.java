package com.comercio.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resumen de pedido para consultas (queryPU). Se actualiza con cada {@code OrderStatusChanged}. */
@Entity
@Table(name = "order_summary_view", indexes = @Index(name = "ix_order_summary_customer", columnList = "customer_id"))
public class OrderSummaryView {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "items_count", nullable = false)
    private int itemsCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "source_version", nullable = false)
    private long sourceVersion;

    protected OrderSummaryView() {
    }

    public OrderSummaryView(UUID orderId) {
        this.orderId = orderId;
    }

    public void apply(String customerId, String status, BigDecimal total, int itemsCount, Instant updatedAt,
                      long sourceVersion) {
        this.customerId = customerId;
        this.status = status;
        this.total = total;
        this.itemsCount = itemsCount;
        this.updatedAt = updatedAt;
        this.sourceVersion = sourceVersion;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getSourceVersion() {
        return sourceVersion;
    }
}
