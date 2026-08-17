package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;
import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.ai.service.AiBriefPersistenceService;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiBriefPersistenceServiceTest {

    private final AiJourneyBriefRepository repository = mock(AiJourneyBriefRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final ClientAdvisorRepository clientAdvisorRepository = mock(ClientAdvisorRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T03:45:00Z"), ZoneId.of("Asia/Seoul"));
    private final AiBriefPersistenceService service = new AiBriefPersistenceService(
        repository,
        customerRepository,
        visitRepository,
        clientAdvisorRepository,
        clock
    );

    @Test
    void saveGeneratedUsesInjectedClock() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        Customer customer = mock(Customer.class);
        Visit visit = mock(Visit.class);
        ClientAdvisor ca = mock(ClientAdvisor.class);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            2
        );
        GeminiBriefResult result = new GeminiBriefResult("s", "p", "i", "c", "d");
        when(customerRepository.findById(20L)).thenReturn(java.util.Optional.of(customer));
        when(visitRepository.findById(30L)).thenReturn(java.util.Optional.of(visit));
        when(clientAdvisorRepository.findById(2L)).thenReturn(java.util.Optional.of(ca));
        when(repository.save(any(AiJourneyBrief.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiJourneyBrief saved = service.saveGenerated(user, 20L, 30L, source, result);

        assertThat(saved.getGeneratedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(saved.getStatus()).isEqualTo(BriefStatus.GENERATED);
        assertThat(saved.getSourceVisitCount()).isEqualTo(2);
    }

    @Test
    void saveFailedUsesInjectedClock() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        Customer customer = mock(Customer.class);
        Visit visit = mock(Visit.class);
        ClientAdvisor ca = mock(ClientAdvisor.class);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            1
        );
        when(customerRepository.findById(20L)).thenReturn(java.util.Optional.of(customer));
        when(visitRepository.findById(30L)).thenReturn(java.util.Optional.of(visit));
        when(clientAdvisorRepository.findById(2L)).thenReturn(java.util.Optional.of(ca));
        when(repository.save(any(AiJourneyBrief.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiJourneyBrief saved = service.saveFailed(user, 20L, 30L, source, ErrorCode.AI_API_FAILED);

        assertThat(saved.getGeneratedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(saved.getStatus()).isEqualTo(BriefStatus.FAILED);
        assertThat(saved.getSummary()).isNull();
    }
}
