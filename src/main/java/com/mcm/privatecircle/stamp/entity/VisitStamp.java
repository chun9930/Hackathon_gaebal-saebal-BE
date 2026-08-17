package com.mcm.privatecircle.stamp.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.visit.entity.Visit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "visit_stamps",
    uniqueConstraints = @UniqueConstraint(name = "uk_visit_stamps_visit", columnNames = "visit_id")
)
public class VisitStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_ca_id", nullable = false)
    private ClientAdvisor issuedByCa;

    @Column(name = "stamp_type", nullable = false, length = 30)
    private String stampType;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    protected VisitStamp() {
    }

    public VisitStamp(
        Visit visit,
        Customer customer,
        ClientAdvisor issuedByCa,
        String stampType,
        LocalDateTime issuedAt
    ) {
        this.visit = visit;
        this.customer = customer;
        this.issuedByCa = issuedByCa;
        this.stampType = stampType;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public Visit getVisit() {
        return visit;
    }

    public Customer getCustomer() {
        return customer;
    }

    public ClientAdvisor getIssuedByCa() {
        return issuedByCa;
    }

    public String getStampType() {
        return stampType;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
}
