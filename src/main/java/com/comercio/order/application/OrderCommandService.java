package com.comercio.order.application;

import com.comercio.catalog.application.ProductPricingService;
import com.comercio.order.domain.Order;
import com.comercio.order.domain.OrderStatus;
import com.comercio.order.infrastructure.OrderRepository;
import com.comercio.shared.event.OrderPlaced;
import com.comercio.shared.event.OrderStatusChanged;
import com.comercio.shared.exception.OrderNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/** Comandos sobre pedidos (ordersPU). */
@ApplicationScoped
public class OrderCommandService {

    private static final Logger LOG = Logger.getLogger(OrderCommandService.class.getName());

    @Inject
    OrderRepository orders;

    @Inject
    ProductPricingService pricing;

    @Inject
    Event<OrderStatusChanged> statusChanged;

    @Inject
    Event<OrderPlaced> orderPlaced;

    /**
     * Registra el pedido en PENDIENTE. La SAGA no se lanza aquí: se publica {@link OrderPlaced} y su
     * observador (AFTER_SUCCESS) la arranca solo si esta transacción hace commit.
     */
    @Transactional
    public Order placeOrder(String customerId, String paymentMethod, List<PlaceOrderLine> lines) {
        // Una línea por producto: la reserva de inventario es única por (pedido, producto)
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        lines.forEach(l -> quantities.merge(l.productId(), l.quantity(), Integer::sum));

        Order order = new Order(customerId, paymentMethod);
        quantities.forEach((productId, qty) -> order.addItem(productId, qty, pricing.currentPrice(productId)));
        orders.save(order);
        orders.flush();
        LOG.info(() -> "Pedido " + order.getId() + " registrado en PENDIENTE, total=" + order.getTotal());

        publishStatusChanged(order, null);
        orderPlaced.fire(new OrderPlaced(order.getId()));
        return order;
    }

    /**
     * Transición de estado del pedido. {@code MANDATORY}: la invoca un paso de la SAGA dentro de su
     * transacción JTA, de modo que el estado del pedido se confirma junto con el cambio en el otro dominio.
     */
    @Transactional(TxType.MANDATORY)
    public Order changeStatus(UUID orderId, OrderStatus target) {
        Order order = get(orderId);
        OrderStatus previous = order.getStatus();
        order.transitionTo(target);
        orders.flush();
        LOG.info(() -> "Pedido " + orderId + ": " + previous + " -> " + target);
        publishStatusChanged(order, previous);
        return order;
    }

    @Transactional(TxType.SUPPORTS)
    public Order get(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void publishStatusChanged(Order order, OrderStatus previous) {
        statusChanged.fire(new OrderStatusChanged(order.getId(), order.getCustomerId(),
                previous == null ? null : previous.name(), order.getStatus().name(), order.getTotal(),
                order.itemsCount(), order.getVersion(), order.getUpdatedAt()));
    }
}
