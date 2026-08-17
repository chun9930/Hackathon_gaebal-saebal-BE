package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.mcm.privatecircle.ai.client.GeminiBriefClient;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AiBriefServiceNoGeminiTest {

    private final AiJourneyBriefRepository repository = mock(AiJourneyBriefRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final GeminiBriefClient geminiBriefClient = mock(GeminiBriefClient.class);
    private final AiBriefService service = new AiBriefService(
        repository,
        visitRepository,
        customerRepository,
        geminiBriefClient
    );

    @Test
    void getLatestDoesNotCallGemini() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        Customer customer = mock(Customer.class);
        Visit visit = mock(Visit.class);
        AiJourneyBrief brief = brief(customer, visit, BriefStatus.GENERATED, 1L);

        when(visitRepository.findByIdAndStoreId(30L, 3L)).thenReturn(Optional.of(visit));
        when(visit.belongsToCustomer(20L)).thenReturn(true);
        when(repository.findTopByCustomerIdAndVisitIdOrderByGeneratedAtDescIdDesc(20L, 30L))
            .thenReturn(Optional.of(brief));

        AiBriefResponse response = service.getLatest(user, 20L, 30L);

        assertThat(response.briefId()).isEqualTo(1L);
        verifyNoInteractions(geminiBriefClient);
    }

    @Test
    void getHistoryDoesNotCallGemini() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        Customer customer = mock(Customer.class);
        Visit visit = mock(Visit.class);
        AiJourneyBrief brief = brief(customer, visit, BriefStatus.FAILED, 2L);

        when(customerRepository.existsById(20L)).thenReturn(true);
        when(repository.findByCustomerIdAndVisitStoreId(
            20L, 3L, PageRequest.of(0, 20, AiBriefService.HISTORY_SORT)
        )).thenReturn(new PageImpl<>(List.of(brief)));

        PageResponse<AiBriefResponse> page = service.getHistory(user, 20L, 0, 20);

        assertThat(page.items()).hasSize(1);
        verifyNoInteractions(geminiBriefClient);
    }

    private AiJourneyBrief brief(Customer customer, Visit visit, BriefStatus status, Long id) {
        return new AiJourneyBrief(
            id,
            customer,
            visit,
            null,
            "summary",
            "purpose",
            "interest",
            "caution",
            "direction",
            3,
            status,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );
    }
}
