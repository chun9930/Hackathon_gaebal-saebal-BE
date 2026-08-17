package com.mcm.privatecircle.purchase.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.visit.entity.Visit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_history")
public class PurchaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    protected PurchaseHistory() {
    }

    public PurchaseHistory(
        Customer customer,
        Product product,
        Store store,
        Visit visit,
        Integer quantity,
        LocalDateTime purchasedAt
    ) {
        this.customer = customer;
        this.product = product;
        this.store = store;
        this.visit = visit;
        this.quantity = quantity;
        this.purchasedAt = purchasedAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Product getProduct() {
        return product;
    }

    public Store getStore() {
        return store;
    }

    public Visit getVisit() {
        return visit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}
