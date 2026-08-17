package com.mcm.privatecircle.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.auth.dto.AuthTokenResponse;
import com.mcm.privatecircle.auth.dto.CustomerLoginRequest;
import com.mcm.privatecircle.auth.dto.CustomerSignupRequest;
import com.mcm.privatecircle.auth.dto.EmployeeLoginRequest;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.JwtTokenProvider;
import com.mcm.privatecircle.global.security.UserRole;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class AuthService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final CustomerAccountRepository customerAccountRepository;
	private final EmployeeAccountRepository employeeAccountRepository;
	private final CustomerRepository customerRepository;
	private final ClientAdvisorRepository clientAdvisorRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
		CustomerAccountRepository customerAccountRepository,
		EmployeeAccountRepository employeeAccountRepository,
		CustomerRepository customerRepository,
		ClientAdvisorRepository clientAdvisorRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenProvider jwtTokenProvider
	) {
		this.customerAccountRepository = customerAccountRepository;
		this.employeeAccountRepository = employeeAccountRepository;
		this.customerRepository = customerRepository;
		this.clientAdvisorRepository = clientAdvisorRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	public AuthTokenResponse signup(CustomerSignupRequest request) {
		if (customerAccountRepository.existsByLoginId(request.loginId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (customerRepository.existsByPhoneNumber(request.phoneNumber())) {
			throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
		}

		CustomerAccount account = customerAccountRepository.save(
			new CustomerAccount(request.loginId(), passwordEncoder.encode(request.password()))
		);
		Customer customer = customerRepository.save(
			new Customer(
				account,
				null,
				request.name(),
				request.phoneNumber(),
				null,
				null,
				generateUniqueQrToken(),
				null,
				LocalDateTime.now(KST)
			)
		);

		AuthenticatedUser authenticatedUser = AuthenticatedUser.customer(account.getId(), customer.getId());
		String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
		return new AuthTokenResponse(
			accessToken,
			"Bearer",
			account.getId(),
			customer.getId(),
			null,
			null,
			UserRole.CUSTOMER
		);
	}

	public AuthTokenResponse loginCustomer(CustomerLoginRequest request) {
		CustomerAccount account = customerAccountRepository.findByLoginId(request.loginId())
			.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		Customer customer = customerRepository.findByCustomerAccountId(account.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

		AuthenticatedUser authenticatedUser = AuthenticatedUser.customer(account.getId(), customer.getId());
		String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
		return new AuthTokenResponse(
			accessToken,
			"Bearer",
			account.getId(),
			customer.getId(),
			null,
			null,
			UserRole.CUSTOMER
		);
	}

	public AuthTokenResponse loginEmployee(EmployeeLoginRequest request) {
		EmployeeAccount account = employeeAccountRepository.findByLoginId(request.loginId())
			.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		ClientAdvisor advisor = clientAdvisorRepository.findByEmployeeAccountId(account.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.CA_NOT_FOUND));

		AuthenticatedUser authenticatedUser = AuthenticatedUser.ca(
			account.getId(),
			advisor.getId(),
			advisor.getStore().getId()
		);
		String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
		return new AuthTokenResponse(
			accessToken,
			"Bearer",
			account.getId(),
			null,
			advisor.getId(),
			advisor.getStore().getId(),
			UserRole.CA
		);
	}

	private String generateUniqueQrToken() {
		String qrToken;
		do {
			qrToken = UUID.randomUUID().toString().replace("-", "");
		} while (customerRepository.existsByQrToken(qrToken));
		return qrToken;
	}
}
