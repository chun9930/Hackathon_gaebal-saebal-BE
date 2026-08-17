package com.mcm.privatecircle.employee.service;

import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.employee.dto.EmployeeProfileResponse;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class EmployeeService {

	private final ClientAdvisorRepository clientAdvisorRepository;
	private final EmployeeAccountRepository employeeAccountRepository;

	public EmployeeService(
		ClientAdvisorRepository clientAdvisorRepository,
		EmployeeAccountRepository employeeAccountRepository
	) {
		this.clientAdvisorRepository = clientAdvisorRepository;
		this.employeeAccountRepository = employeeAccountRepository;
	}

	public EmployeeProfileResponse getMyProfile(AuthenticatedUser authenticatedUser) {
		ClientAdvisor advisor = clientAdvisorRepository.findByEmployeeAccountId(authenticatedUser.getAccountId())
			.orElseThrow(() -> new BusinessException(ErrorCode.CA_NOT_FOUND));
		EmployeeAccount account = employeeAccountRepository.findById(authenticatedUser.getAccountId())
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

		return new EmployeeProfileResponse(
			advisor.getId(),
			advisor.getStore().getId(),
			advisor.getStore().getName(),
			advisor.getName(),
			account.getId(),
			account.getLoginId(),
			advisor.getCreatedAt()
		);
	}
}
