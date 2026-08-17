package com.mcm.privatecircle.visit.entity;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.global.common.entity.BaseTimeEntity;

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
    name = "visit_records",
    uniqueConstraints = @UniqueConstraint(name = "uk_visit_records_visit", columnNames = "visit_id")
)
public class VisitRecord extends BaseTimeEntity {

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
    @JoinColumn(name = "ca_id", nullable = false)
    private ClientAdvisor ca;

    @Column(name = "visit_purpose", length = 255)
    private String visitPurpose;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "style_change_note", columnDefinition = "text")
    private String styleChangeNote;

    @Column(name = "caution_note", columnDefinition = "text")
    private String cautionNote;

    protected VisitRecord() {
    }

    public VisitRecord(
        Visit visit,
        Customer customer,
        ClientAdvisor ca,
        String visitPurpose,
        String content,
        String styleChangeNote,
        String cautionNote
    ) {
        this.visit = visit;
        this.customer = customer;
        this.ca = ca;
        this.visitPurpose = visitPurpose;
        this.content = content;
        this.styleChangeNote = styleChangeNote;
        this.cautionNote = cautionNote;
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

    public ClientAdvisor getCa() {
        return ca;
    }

    public String getVisitPurpose() {
        return visitPurpose;
    }

    public String getContent() {
        return content;
    }

    public String getStyleChangeNote() {
        return styleChangeNote;
    }

    public String getCautionNote() {
        return cautionNote;
    }

    public boolean isAuthoredBy(Long caId) {
        return caId != null && caId.equals(ca.getId());
    }

    public void update(
        String visitPurpose,
        String content,
        String styleChangeNote,
        String cautionNote
    ) {
        if (visitPurpose != null) {
            this.visitPurpose = visitPurpose;
        }
        if (content != null) {
            this.content = content;
        }
        if (styleChangeNote != null) {
            this.styleChangeNote = styleChangeNote;
        }
        if (cautionNote != null) {
            this.cautionNote = cautionNote;
        }
    }
}
