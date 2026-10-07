package com.comercio.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resultado de un cobro (APROBADO o RECHAZADO). */
public record PaymentProcessed(Long paymentId, UUID orderId, BigDecimal amount, String status, String reason,
                               Instant processedAt) {
}
