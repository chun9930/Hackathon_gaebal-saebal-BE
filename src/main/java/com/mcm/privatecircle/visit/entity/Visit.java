package com.mcm.privatecircle.visit.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.store.entity.Store;

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
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    protected Visit() {
    }

    public Visit(Customer customer, Store store, LocalDateTime visitedAt) {
        this.customer = customer;
        this.store = store;
        this.visitedAt = visitedAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Store getStore() {
        return store;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public boolean belongsToCustomer(Long customerId) {
        return customerId != null && customerId.equals(customer.getId());
    }

    public boolean belongsToStore(Long storeId) {
        return storeId != null && storeId.equals(store.getId());
    }
}
