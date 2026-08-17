package com.mcm.privatecircle.customer.service;

import com.mcm.privatecircle.customer.dto.CustomerActivitySummary;
import com.mcm.privatecircle.stamp.repository.VisitStampRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DomainCustomerActivitySummaryReader implements CustomerActivitySummaryReader {

    private final VisitRepository visitRepository;
    private final VisitStampRepository stampRepository;

    public DomainCustomerActivitySummaryReader(
        VisitRepository visitRepository,
        VisitStampRepository stampRepository
    ) {
        this.visitRepository = visitRepository;
        this.stampRepository = stampRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerActivitySummary read(Long customerId) {
        return new CustomerActivitySummary(
            visitRepository.countByCustomerId(customerId),
            stampRepository.countByCustomerId(customerId),
            visitRepository.findTopByCustomerIdOrderByVisitedAtDescIdDesc(customerId)
                .map(visit -> visit.getVisitedAt())
                .orElse(null)
        );
    }
}
