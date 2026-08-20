package com.mcm.privatecircle.ai.service;

import com.mcm.privatecircle.ai.client.AiClientException;
import com.mcm.privatecircle.ai.client.AiClientAuthenticationException;
import com.mcm.privatecircle.ai.client.AiClientConfigurationException;
import com.mcm.privatecircle.ai.client.AiClientTimeoutException;
import com.mcm.privatecircle.ai.client.AiResponseParseException;
import com.mcm.privatecircle.ai.client.GeminiBriefClient;
import com.mcm.privatecircle.ai.dto.AiBriefCreateRequest;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiBriefService {

    private static final Logger log = LoggerFactory.getLogger(AiBriefService.class);

    public static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("generatedAt"),
        Sort.Order.desc("id")
    );

    private final AiJourneyBriefRepository repository;
    private final VisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    private final GeminiBriefClient geminiBriefClient;
    private final AiBriefSourceReader sourceReader;
    private final AiBriefPersistenceService persistenceService;

    public AiBriefService(
        AiJourneyBriefRepository repository,
        VisitRepository visitRepository,
        CustomerRepository customerRepository,
        GeminiBriefClient geminiBriefClient,
        AiBriefSourceReader sourceReader,
        AiBriefPersistenceService persistenceService
    ) {
        this.repository = repository;
        this.visitRepository = visitRepository;
        this.customerRepository = customerRepository;
        this.geminiBriefClient = geminiBriefClient;
        this.sourceReader = sourceReader;
        this.persistenceService = persistenceService;
    }

    @Transactional(readOnly = true)
    public AiBriefResponse getLatest(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        Long visitId
    ) {
        requireCa(authenticatedUser);
        Visit visit = visitRepository.findByIdAndStoreId(visitId, authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        if (!visit.belongsToCustomer(customerId)) {
            throw new BusinessException(ErrorCode.VISIT_CUSTOMER_MISMATCH);
        }
        return repository.findTopByCustomerIdAndVisitIdOrderByGeneratedAtDescIdDesc(customerId, visitId)
            .map(AiBriefResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.AI_BRIEF_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResponse<AiBriefResponse> getHistory(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        int page,
        int size
    ) {
        requireCa(authenticatedUser);
        PaginationValidator.validate(page, size);
        if (!customerRepository.existsById(customerId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return PageResponse.from(
            repository.findByCustomerIdAndVisitStoreId(
                customerId,
                authenticatedUser.getStoreId(),
                PageRequest.of(page, size, HISTORY_SORT)
            ).map(AiBriefResponse::from)
        );
    }

    public AiBriefResponse create(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        AiBriefCreateRequest request
    ) {
        requireCa(authenticatedUser);
        if (request == null || request.visitId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        log.info("[AI BRIEF] Source loading started: customerId={}, visitId={}, caId={}, storeId={}",
            customerId, request.visitId(), authenticatedUser.getCaId(), authenticatedUser.getStoreId());
        var source = sourceReader.read(authenticatedUser, customerId, request.visitId());
        log.info("[AI BRIEF] Source loaded: customerId={}, visitId={}, sourceVisitCount={}, interests={}, purchases={}",
            customerId, request.visitId(), source.sourceVisitCount(), source.interestProducts().size(), source.purchases().size());
        log.info("[AI BRIEF] Current visit record present: customerId={}, visitId={}, present={}",
            customerId, request.visitId(), source.currentVisitRecord() != null);
        try {
            log.info("[AI BRIEF] Gemini generation started: customerId={}, visitId={}", customerId, request.visitId());
            var result = geminiBriefClient.generate(source);
            var response = AiBriefResponse.from(
                persistenceService.saveGenerated(authenticatedUser, customerId, request.visitId(), source, result)
            );
            log.info("[AI BRIEF] Generation completed: briefId={}, customerId={}, visitId={}, status={}",
                response.briefId(), response.customerId(), response.visitId(), response.status());
            return response;
        } catch (AiClientConfigurationException exception) {
            log.warn("[AI BRIEF] Gemini API key missing: customerId={}, visitId={}", customerId, request.visitId());
            persistenceService.saveFailed(authenticatedUser, customerId, request.visitId(), source, ErrorCode.AI_BRIEF_API_KEY_MISSING);
            throw new BusinessException(ErrorCode.AI_BRIEF_API_KEY_MISSING, exception);
        } catch (AiClientAuthenticationException exception) {
            log.warn("[AI BRIEF] Gemini authentication failed: customerId={}, visitId={}", customerId, request.visitId());
            persistenceService.saveFailed(authenticatedUser, customerId, request.visitId(), source, ErrorCode.AI_BRIEF_AUTH_FAILED);
            throw new BusinessException(ErrorCode.AI_BRIEF_AUTH_FAILED, exception);
        } catch (AiClientTimeoutException exception) {
            log.warn("[AI BRIEF] Gemini timeout: customerId={}, visitId={}", customerId, request.visitId());
            persistenceService.saveFailed(authenticatedUser, customerId, request.visitId(), source, ErrorCode.AI_API_TIMEOUT);
            throw new BusinessException(ErrorCode.AI_API_TIMEOUT, exception);
        } catch (AiResponseParseException exception) {
            log.warn("[AI BRIEF] Gemini response parse failed: customerId={}, visitId={}", customerId, request.visitId());
            persistenceService.saveFailed(authenticatedUser, customerId, request.visitId(), source, ErrorCode.AI_RESPONSE_PARSE_FAILED);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED, exception);
        } catch (AiClientException exception) {
            log.warn("[AI BRIEF] Gemini API failed: customerId={}, visitId={}, message={}",
                customerId, request.visitId(), exception.getMessage());
            persistenceService.saveFailed(authenticatedUser, customerId, request.visitId(), source, ErrorCode.AI_API_FAILED);
            throw new BusinessException(ErrorCode.AI_API_FAILED, exception);
        }
    }

    private void requireCa(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null
            || authenticatedUser.getRole() != UserRole.CA
            || authenticatedUser.getCaId() == null
            || authenticatedUser.getStoreId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
