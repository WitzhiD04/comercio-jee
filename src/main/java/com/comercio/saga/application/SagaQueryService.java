package com.comercio.saga.application;

import com.comercio.saga.domain.SagaInstance;
import com.comercio.saga.infrastructure.SagaInstanceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

/**
 * Traza de la SAGA. Es la única consulta que lee el modelo de escritura (ordersPU): la bitácora es un
 * registro de auditoría del propio orquestador y no tiene proyección en queryPU.
 */
@ApplicationScoped
public class SagaQueryService {

    @Inject
    SagaInstanceRepository sagas;

    public Optional<SagaInstance> findByOrder(UUID orderId) {
        return sagas.findByOrderId(orderId);
    }
}
