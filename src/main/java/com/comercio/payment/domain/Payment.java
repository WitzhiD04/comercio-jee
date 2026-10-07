package com.comercio.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pago de un pedido (paymentsPU). {@code order_id} es único: procesar dos veces el pago del mismo
 * pedido (p. ej. al reintentar el paso de la SAGA) devuelve el pago existente en vez de cobrar de nuevo.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected Payment() {
    }

    private Payment(UUID orderId, BigDecimal amount, PaymentStatus status, String reason) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.processedAt = Instant.now();
    }

    public static Payment approved(UUID orderId, BigDecimal amount) {
        return new Payment(orderId, amount, PaymentStatus.APROBADO, "Pago aprobado");
    }

    public static Payment rejected(UUID orderId, BigDecimal amount, String reason) {
        return new Payment(orderId, amount, PaymentStatus.RECHAZADO, reason);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APROBADO;
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
