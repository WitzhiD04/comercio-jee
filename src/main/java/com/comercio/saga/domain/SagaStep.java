package com.comercio.saga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Entrada de la bitácora de la SAGA: qué paso se ejecutó o compensó y con qué resultado. */
@Entity
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaInstance saga;

    @NotBlank
    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 12)
    private StepAction action;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 8)
    private StepResult result;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant timestamp;

    protected SagaStep() {
    }

    SagaStep(SagaInstance saga, String name, StepAction action, StepResult result, String detail) {
        this.saga = saga;
        this.name = name;
        this.action = action;
        this.result = result;
        this.detail = detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StepAction getAction() {
        return action;
    }

    public StepResult getResult() {
        return result;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
