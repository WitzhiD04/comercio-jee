package com.comercio.saga.domain;

public enum SagaStatus {
    INICIADA,
    COMPENSANDO,
    COMPLETADA,
    COMPENSADA,
    FALLIDA
}
