package com.comercio.saga.api;

import com.comercio.saga.application.SagaQueryService;
import com.comercio.saga.domain.SagaInstance;
import com.comercio.saga.domain.SagaStep;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** GET /orders/{orderId}/saga: estado de la SAGA y su bitácora de pasos (lee ordersPU). */
@Path("orders/{orderId}/saga")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SagaQueryResource {

    @Inject
    SagaQueryService queries;

    public record SagaStepView(String name, String action, String result, String detail, Instant timestamp) {

        static SagaStepView from(SagaStep s) {
            return new SagaStepView(s.getName(), s.getAction().name(), s.getResult().name(), s.getDetail(),
                    s.getTimestamp());
        }
    }

    public record SagaTrace(Long sagaId, UUID orderId, String status, String currentStep, Instant startedAt,
                            Instant updatedAt, List<SagaStepView> steps) {

        static SagaTrace from(SagaInstance s) {
            return new SagaTrace(s.getId(), s.getOrderId(), s.getStatus().name(), s.getCurrentStep(),
                    s.getStartedAt(), s.getUpdatedAt(), s.getSteps().stream().map(SagaStepView::from).toList());
        }
    }

    @GET
    public SagaTrace get(@PathParam("orderId") UUID orderId) {
        return queries.findByOrder(orderId)
                .map(SagaTrace::from)
                .orElseThrow(() -> new NotFoundException("No hay SAGA registrada para el pedido " + orderId));
    }
}
