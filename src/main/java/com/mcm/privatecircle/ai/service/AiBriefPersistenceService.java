package com.mcm.privatecircle.ai.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;
import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiBriefPersistenceService {

    private final AiJourneyBriefRepository repository;
    private final CustomerRepository customerRepository;
    private final VisitRepository visitRepository;
    private final ClientAdvisorRepository clientAdvisorRepository;
    private final Clock clock;

    public AiBriefPersistenceService(
        AiJourneyBriefRepository repository,
        CustomerRepository customerRepository,
        VisitRepository visitRepository,
        ClientAdvisorRepository clientAdvisorRepository,
        Clock clock
    ) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.visitRepository = visitRepository;
        this.clientAdvisorRepository = clientAdvisorRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiJourneyBrief saveGenerated(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        Long visitId,
        AiBriefSource source,
        GeminiBriefResult result
    ) {
        Customer customer = findCustomer(customerId);
        Visit visit = findVisit(visitId);
        ClientAdvisor ca = findCa(authenticatedUser.getCaId());
        return repository.save(new AiJourneyBrief(
            null,
            customer,
            visit,
            ca,
            result.summary(),
            result.visitPurposeSummary(),
            result.interestSummary(),
            result.cautionSummary(),
            result.suggestedDirection(),
            source.sourceVisitCount(),
            BriefStatus.GENERATED,
            LocalDateTime.now(clock)
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiJourneyBrief saveFailed(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        Long visitId,
        AiBriefSource source,
        ErrorCode errorCode
    ) {
        Customer customer = findCustomer(customerId);
        Visit visit = findVisit(visitId);
        ClientAdvisor ca = findCa(authenticatedUser.getCaId());
        return repository.save(new AiJourneyBrief(
            null,
            customer,
            visit,
            ca,
            null,
            null,
            null,
            null,
            null,
            source.sourceVisitCount(),
            BriefStatus.FAILED,
            LocalDateTime.now(clock)
        ));
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Visit findVisit(Long visitId) {
        return visitRepository.findById(visitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
    }

    private ClientAdvisor findCa(Long caId) {
        return clientAdvisorRepository.findById(caId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CA_NOT_FOUND));
    }
}
