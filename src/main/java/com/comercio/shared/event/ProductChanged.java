package com.comercio.shared.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alta o cambio de datos de un producto (nombre, precio, activo...). Lleva la foto completa del
 * producto para que la proyección pueda crear o reemplazar la vista sin consultar el modelo de escritura.
 * {@code version} es el {@code @Version} de Product tras el cambio: la proyección ignora eventos más viejos.
 */
public record ProductChanged(Long productId, String sku, String name, BigDecimal price,
                             int stock, int reserved, boolean active, long version, Instant occurredAt) {
}
