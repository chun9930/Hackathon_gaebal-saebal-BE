package com.mcm.privatecircle.interest.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.visit.entity.VisitRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "customer_interest_products",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_interest_visit_record_product",
        columnNames = {"visit_record_id", "product_id"}
    )
)
public class CustomerInterestProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private InterestSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_record_id")
    private VisitRecord visitRecord;

    @Column(length = 500)
    private String memo;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    protected CustomerInterestProduct() {
    }

    public CustomerInterestProduct(
        Customer customer,
        Product product,
        InterestSourceType sourceType,
        VisitRecord visitRecord,
        String memo,
        LocalDateTime savedAt
    ) {
        this.customer = customer;
        this.product = product;
        this.sourceType = sourceType;
        this.visitRecord = visitRecord;
        this.memo = memo;
        this.savedAt = savedAt;
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

    public InterestSourceType getSourceType() {
        return sourceType;
    }

    public VisitRecord getVisitRecord() {
        return visitRecord;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public boolean isCustomerSourceOwnedBy(Long customerId) {
        return sourceType == InterestSourceType.CUSTOMER
            && customerId != null
            && customerId.equals(customer.getId());
    }

    public boolean isCaSourceOwnedBy(Long caId, Long storeId) {
        return sourceType == InterestSourceType.CA
            && visitRecord != null
            && visitRecord.isAuthoredBy(caId)
            && visitRecord.getVisit().belongsToStore(storeId);
    }
}
