package com.mcm.privatecircle.ai.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.visit.entity.Visit;

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

@Entity
@Table(name = "ai_journey_briefs")
public class AiJourneyBrief {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_ca_id", nullable = false)
    private ClientAdvisor requestedByCa;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "visit_purpose_summary", columnDefinition = "text")
    private String visitPurposeSummary;

    @Column(name = "interest_summary", columnDefinition = "text")
    private String interestSummary;

    @Column(name = "caution_summary", columnDefinition = "text")
    private String cautionSummary;

    @Column(name = "suggested_direction", columnDefinition = "text")
    private String suggestedDirection;

    @Column(name = "source_visit_count", nullable = false)
    private Integer sourceVisitCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BriefStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected AiJourneyBrief() {
    }

    public AiJourneyBrief(
        Long id,
        Customer customer,
        Visit visit,
        ClientAdvisor requestedByCa,
        String summary,
        String visitPurposeSummary,
        String interestSummary,
        String cautionSummary,
        String suggestedDirection,
        Integer sourceVisitCount,
        BriefStatus status,
        LocalDateTime generatedAt
    ) {
        this.id = id;
        this.customer = customer;
        this.visit = visit;
        this.requestedByCa = requestedByCa;
        this.summary = summary;
        this.visitPurposeSummary = visitPurposeSummary;
        this.interestSummary = interestSummary;
        this.cautionSummary = cautionSummary;
        this.suggestedDirection = suggestedDirection;
        this.sourceVisitCount = sourceVisitCount;
        this.status = status;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Visit getVisit() {
        return visit;
    }

    public ClientAdvisor getRequestedByCa() {
        return requestedByCa;
    }

    public String getSummary() {
        return summary;
    }

    public String getVisitPurposeSummary() {
        return visitPurposeSummary;
    }

    public String getInterestSummary() {
        return interestSummary;
    }

    public String getCautionSummary() {
        return cautionSummary;
    }

    public String getSuggestedDirection() {
        return suggestedDirection;
    }

    public Integer getSourceVisitCount() {
        return sourceVisitCount;
    }

    public BriefStatus getStatus() {
        return status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
