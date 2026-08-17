package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.visit.dto.VisitRecordCreateRequest;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;
import com.mcm.privatecircle.visit.service.VisitRecordService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VisitRecordServiceConstraintTest {

    private final VisitRecordRepository visitRecordRepository = mock(VisitRecordRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final ClientAdvisorRepository clientAdvisorRepository = mock(ClientAdvisorRepository.class);
    private final VisitRecordService service = new VisitRecordService(
        visitRecordRepository,
        visitRepository,
        clientAdvisorRepository
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
        when(visitRecordRepository.existsByVisitId(10L)).thenReturn(false);
    }

    @Test
    void 알려진_방문별_Unique_충돌만_도메인_409로_변환한다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "저장 실패",
            new RuntimeException("uk_visit_records_visit")
        );
        when(visitRecordRepository.saveAndFlush(any(VisitRecord.class))).thenThrow(exception);

        assertThatThrownBy(() -> service.createVisitRecord(caUser, 10L, request()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_RECORD_ALREADY_EXISTS);
    }

    @Test
    void 알_수_없는_DB_오류는_중복_오류로_변환하지_않는다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "unknown_constraint"
        );
        when(visitRecordRepository.saveAndFlush(any(VisitRecord.class))).thenThrow(exception);

        assertThatThrownBy(() -> service.createVisitRecord(caUser, 10L, request()))
            .isSameAs(exception);
    }

    private VisitRecordCreateRequest request() {
        return new VisitRecordCreateRequest("방문 목적", "상담 내용", null, null);
    }
}
