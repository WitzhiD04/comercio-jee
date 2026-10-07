package com.comercio.catalog.domain;

import com.comercio.shared.exception.BusinessRuleException;
import com.comercio.shared.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Producto del catálogo (inventoryPU).
 *
 * <p>{@code stock} son las existencias físicas; {@code reserved} es la suma de las reservas en estado
 * RESERVADA (se mantiene junto al detalle de {@link InventoryReservation} para no recalcularla en cada
 * consulta). Disponible = stock − reserved.
 *
 * <p>Concurrencia: las operaciones de inventario de la SAGA bloquean la fila con
 * {@code LockModeType.PESSIMISTIC_WRITE} (SELECT ... FOR UPDATE). {@code @Version} protege además las
 * ediciones administrativas (PUT/PATCH) frente a actualizaciones perdidas y ordena las proyecciones CQRS.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "sku", nullable = false, unique = true, length = 64)
    private String sku;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Min(0)
    @Column(name = "stock", nullable = false)
    private int stock;

    @Min(0)
    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal price, int stock, boolean active) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.active = active;
    }

    public int available() {
        return stock - reserved;
    }

    /** Aparta unidades para un pedido; no toca las existencias físicas. */
    public void reserve(int quantity) {
        if (!active) {
            throw new BusinessRuleException("El producto " + id + " no está activo");
        }
        if (quantity > available()) {
            throw new InsufficientStockException(id, quantity, available());
        }
        reserved += quantity;
    }

    /** Compensación de {@link #reserve(int)}: las unidades vuelven a estar disponibles. */
    public void release(int quantity) {
        reserved = Math.max(0, reserved - quantity);
    }

    /** La reserva se convierte en venta: salen las unidades del stock físico. */
    public void commitReservation(int quantity) {
        reserved = Math.max(0, reserved - quantity);
        stock -= quantity;
    }

    public void changeStock(int newStock) {
        if (newStock < reserved) {
            throw new InsufficientStockException("El stock (" + newStock + ") no puede ser menor que las unidades "
                    + "reservadas (" + reserved + ") del producto " + id);
        }
        this.stock = newStock;
    }

    public void update(String sku, String name, BigDecimal price, int stock, boolean active) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        changeStock(stock);
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public int getReserved() {
        return reserved;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getVersion() {
        return version;
    }
}
