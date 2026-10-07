package com.comercio.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vista de pagos (queryPU), para que GET /payments no lea payments_db. */
@Entity
@Table(name = "payment_view", indexes = @Index(name = "ix_payment_view_order", columnList = "order_id"))
public class PaymentView {

    @Id
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected PaymentView() {
    }

    public PaymentView(Long paymentId, UUID orderId, BigDecimal amount, String status, String reason,
                       Instant processedAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.processedAt = processedAt;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
