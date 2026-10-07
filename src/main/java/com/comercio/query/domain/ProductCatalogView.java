package com.comercio.query.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista desnormalizada del catálogo e inventario (queryPU). Se alimenta solo con eventos; {@code available}
 * ya viene calculado (stock − reserved) para que la consulta sea un simple SELECT.
 * {@code sourceVersion} es el {@code @Version} del Product que originó la foto: evita aplicar eventos viejos.
 */
@Entity
@Table(name = "product_catalog_view")
public class ProductCatalogView {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "source_version", nullable = false)
    private long sourceVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductCatalogView() {
    }

    public ProductCatalogView(Long productId) {
        this.productId = productId;
    }

    public void applyProduct(String sku, String name, BigDecimal price, boolean active) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.active = active;
    }

    public void applyStock(int stock, int reserved) {
        this.stock = stock;
        this.reserved = reserved;
        this.available = stock - reserved;
    }

    public void markVersion(long sourceVersion, Instant updatedAt) {
        this.sourceVersion = sourceVersion;
        this.updatedAt = updatedAt;
    }

    public Long getProductId() {
        return productId;
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

    public int getStock() {
        return stock;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailable() {
        return available;
    }

    public boolean isActive() {
        return active;
    }

    public long getSourceVersion() {
        return sourceVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
