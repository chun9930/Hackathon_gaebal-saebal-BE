package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.mcm.privatecircle.ai.client.AiClientException;
import com.mcm.privatecircle.ai.client.AiClientTimeoutException;
import com.mcm.privatecircle.ai.client.AiResponseParseException;
import com.mcm.privatecircle.ai.client.GeminiBriefClient;
import com.mcm.privatecircle.ai.dto.AiBriefCreateRequest;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;
import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.ai.service.AiBriefPersistenceService;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.ai.service.AiBriefSourceReader;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.Test;

class AiBriefCreateFlowTest {

    private final AiJourneyBriefRepository repository = mock(AiJourneyBriefRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final GeminiBriefClient geminiBriefClient = mock(GeminiBriefClient.class);
    private final AiBriefSourceReader sourceReader = mock(AiBriefSourceReader.class);
    private final AiBriefPersistenceService persistenceService = mock(AiBriefPersistenceService.class);
    private final AiBriefService service = new AiBriefService(
        repository,
        visitRepository,
        customerRepository,
        geminiBriefClient,
        sourceReader,
        persistenceService
    );

    @Test
    void createDoesNotCallGeminiOrSaveFailedOn4xxValidationError() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        when(sourceReader.read(user, 20L, 30L))
            .thenThrow(new BusinessException(ErrorCode.VISIT_NOT_FOUND));

        assertThatThrownBy(() -> service.create(user, 20L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
        verifyNoInteractions(geminiBriefClient);
        verifyNoInteractions(persistenceService);
    }

    @Test
    void createSavesGeneratedBriefOnSuccess() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            0
        );
        GeminiBriefResult result = new GeminiBriefResult(
            "summary",
            "purpose",
            "interest",
            "caution",
            "direction"
        );
        Customer customer = mock(Customer.class);
        Visit visit = mock(Visit.class);
        ClientAdvisor ca = mock(ClientAdvisor.class);
        when(customer.getId()).thenReturn(20L);
        when(visit.getId()).thenReturn(30L);
        when(ca.getId()).thenReturn(2L);
        AiJourneyBrief saved = new AiJourneyBrief(
            10L,
            customer,
            visit,
            ca,
            "summary",
            "purpose",
            "interest",
            "caution",
            "direction",
            0,
            BriefStatus.GENERATED,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );
        when(sourceReader.read(user, 20L, 30L)).thenReturn(source);
        when(geminiBriefClient.generate(source)).thenReturn(result);
        when(persistenceService.saveGenerated(user, 20L, 30L, source, result)).thenReturn(saved);

        AiBriefResponse response = service.create(user, 20L, request);

        assertThat(response.briefId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(BriefStatus.GENERATED);
    }

    @Test
    void createSavesFailedAndThrows502OnExternalFailure() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            0
        );
        when(sourceReader.read(user, 20L, 30L)).thenReturn(source);
        when(geminiBriefClient.generate(source)).thenThrow(new AiClientException("failed"));

        assertThatThrownBy(() -> service.create(user, 20L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.AI_API_FAILED);
        verify(persistenceService).saveFailed(eq(user), eq(20L), eq(30L), eq(source), eq(ErrorCode.AI_API_FAILED));
    }

    @Test
    void createSavesFailedAndThrowsTimeoutOnAiTimeout() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            0
        );
        when(sourceReader.read(user, 20L, 30L)).thenReturn(source);
        when(geminiBriefClient.generate(source)).thenThrow(new AiClientTimeoutException("timeout"));

        assertThatThrownBy(() -> service.create(user, 20L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.AI_API_TIMEOUT);
        verify(persistenceService).saveFailed(eq(user), eq(20L), eq(30L), eq(source), eq(ErrorCode.AI_API_TIMEOUT));
    }

    @Test
    void createSavesFailedAndThrows502OnParseFailure() {
        AuthenticatedUser user = AuthenticatedUser.ca(1L, 2L, 3L);
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        AiBriefSource source = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            0
        );
        when(sourceReader.read(user, 20L, 30L)).thenReturn(source);
        when(geminiBriefClient.generate(source)).thenThrow(new AiResponseParseException("parse"));

        assertThatThrownBy(() -> service.create(user, 20L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        verify(persistenceService).saveFailed(eq(user), eq(20L), eq(30L), eq(source), eq(ErrorCode.AI_RESPONSE_PARSE_FAILED));
    }
}
