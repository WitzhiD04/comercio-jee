package com.comercio.saga.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Instancia de la SAGA de un pedido y su bitácora de pasos (ordersPU). Vive en la misma base que el
 * pedido para que el cambio de estado del pedido y el registro del paso se escriban juntos.
 */
@Entity
@Table(name = "saga_instances")
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SagaStatus status;

    @Column(name = "current_step", length = 40)
    private String currentStep;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "saga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<SagaStep> steps = new ArrayList<>();

    protected SagaInstance() {
    }

    public SagaInstance(UUID orderId) {
        this.orderId = orderId;
        this.status = SagaStatus.INICIADA;
        this.startedAt = Instant.now();
        this.updatedAt = startedAt;
    }

    /** Añade una entrada a la bitácora y actualiza el paso actual. */
    public SagaStep record(String stepName, StepAction action, StepResult result, String detail) {
        SagaStep step = new SagaStep(this, stepName, action, result, detail);
        steps.add(step);
        this.currentStep = stepName;
        this.updatedAt = step.getTimestamp();
        return step;
    }

    public void changeStatus(SagaStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<SagaStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
