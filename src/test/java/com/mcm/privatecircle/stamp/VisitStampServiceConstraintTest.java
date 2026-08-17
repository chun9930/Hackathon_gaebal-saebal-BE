package com.mcm.privatecircle.stamp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.stamp.dto.VisitStampCreateRequest;
import com.mcm.privatecircle.stamp.entity.VisitStamp;
import com.mcm.privatecircle.stamp.repository.VisitStampRepository;
import com.mcm.privatecircle.stamp.service.VisitStampService;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VisitStampServiceConstraintTest {

    private final VisitStampRepository stampRepository = mock(VisitStampRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final ClientAdvisorRepository clientAdvisorRepository = mock(ClientAdvisorRepository.class);
    private final VisitStampService service = new VisitStampService(
        stampRepository,
        visitRepository,
        customerRepository,
        clientAdvisorRepository,
        Clock.system(ZoneId.of("Asia/Seoul"))
    );

    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @BeforeEach
    void setUp() {
        Visit visit = mock(Visit.class);
        Customer customer = mock(Customer.class);
        ClientAdvisor ca = mock(ClientAdvisor.class);
        Store store = mock(Store.class);
        when(visit.getCustomer()).thenReturn(customer);
        when(ca.getStore()).thenReturn(store);
        when(store.getId()).thenReturn(3L);
        when(visitRepository.findByIdAndStoreId(10L, 3L)).thenReturn(Optional.of(visit));
        when(clientAdvisorRepository.findById(2L)).thenReturn(Optional.of(ca));
        when(stampRepository.existsByVisitId(10L)).thenReturn(false);
    }

    @Test
    void 알려진_방문별_Unique_충돌만_중복_409로_변환한다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "저장 실패",
            new RuntimeException("uk_visit_stamps_visit")
        );
        when(stampRepository.saveAndFlush(any(VisitStamp.class))).thenThrow(exception);

        assertThatThrownBy(() -> service.issueStamp(
            caUser, 10L, new VisitStampCreateRequest("VISIT")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STAMP_ALREADY_ISSUED);
    }

    @Test
    void 알_수_없는_DB_오류는_중복으로_변환하지_않는다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "unknown_constraint"
        );
        when(stampRepository.saveAndFlush(any(VisitStamp.class))).thenThrow(exception);

        assertThatThrownBy(() -> service.issueStamp(
            caUser, 10L, new VisitStampCreateRequest("VISIT")
        )).isSameAs(exception);
    }
}
