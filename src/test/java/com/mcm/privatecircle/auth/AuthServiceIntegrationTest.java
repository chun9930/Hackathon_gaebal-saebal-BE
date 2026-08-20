package com.mcm.privatecircle.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.auth.dto.CustomerLoginRequest;
import com.mcm.privatecircle.auth.dto.CustomerSignupRequest;
import com.mcm.privatecircle.auth.dto.EmployeeLoginRequest;
import com.mcm.privatecircle.auth.service.AuthService;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.JwtTokenProvider;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CustomerAccountRepository customerAccountRepository;

	@Autowired
	private EmployeeAccountRepository employeeAccountRepository;

	@Autowired
	private ClientAdvisorRepository clientAdvisorRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void customerSignupCreatesAccountAndCustomerAndToken() {
		var response = authService.signup(new CustomerSignupRequest(
			"customer01",
			"password123!",
			"Kim",
			"01012345678"
		));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
		assertThat(response.customerId()).isNotNull();
		assertThat(response.accountId()).isNotNull();

		Customer customer = customerRepository.findById(response.customerId()).orElseThrow();
		assertThat(customer.getName()).isEqualTo("Kim");
		assertThat(customer.getPhoneNumber()).isEqualTo("01012345678");
		assertThat(customer.getCustomerNo()).isEqualTo(String.format("C%08d", response.customerId()));
		assertThat(customer.getQrToken()).isNotBlank();
		assertThat(customerAccountRepository.findById(response.accountId())).isPresent();
	}


	@Test
	void customerSignupHasNoLowArtificialMemberLimitAndCreatedCustomersCanLogin() {
		for (int index = 0; index < 20; index++) {
			String suffix = String.format("%02d", index);
			var signup = authService.signup(new CustomerSignupRequest(
				"bulkCustomer" + suffix,
				"password123!",
				"Customer " + suffix,
				"010880000" + suffix
			));

			assertThat(signup.accessToken()).isNotBlank();
			assertThat(signup.role()).isEqualTo(UserRole.CUSTOMER);

			Customer customer = customerRepository.findById(signup.customerId()).orElseThrow();
			assertThat(customer.getCustomerNo()).isEqualTo(String.format("C%08d", signup.customerId()));
			assertThat(customer.getQrToken()).isNotBlank();

			var login = authService.loginCustomer(new CustomerLoginRequest("bulkCustomer" + suffix, "password123!"));
			assertThat(login.accessToken()).isNotBlank();
			assertThat(login.customerId()).isEqualTo(signup.customerId());
		}
	}

	@Test
	void duplicateCustomerPhoneNumberIsRejected() {
		authService.signup(new CustomerSignupRequest(
			"customerPhone01",
			"password123!",
			"Kim",
			"01033334444"
		));

		assertThatThrownBy(() -> authService.signup(new CustomerSignupRequest(
			"customerPhone02",
			"password123!",
			"Lee",
			"01033334444"
		)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_PHONE_NUMBER);
	}

	@Test
	void duplicateCustomerLoginIdIsRejected() {
		authService.signup(new CustomerSignupRequest(
			"customer01",
			"password123!",
			"Kim",
			"01012345678"
		));

		assertThatThrownBy(() -> authService.signup(new CustomerSignupRequest(
			"customer01",
			"password123!",
			"Lee",
			"01099998888"
		)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
	}

	@Test
	void customerLoginReturnsBearerToken() {
		authService.signup(new CustomerSignupRequest(
			"customer02",
			"password123!",
			"Lee",
			"01022223333"
		));

		var response = authService.loginCustomer(new CustomerLoginRequest("customer02", "password123!"));

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
		assertThat(response.accessToken()).isNotBlank();
	}

	@Test
	void invalidCustomerLoginReturnsInvalidCredentials() {
		assertThatThrownBy(() -> authService.loginCustomer(new CustomerLoginRequest("missing", "wrong")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void employeeLoginReturnsCaClaims() {
		Store store = storeRepository.save(new Store("Gangnam", "Seoul"));
		EmployeeAccount employeeAccount = employeeAccountRepository.save(
			new EmployeeAccount("employee01", passwordEncoder.encode("password123!"))
		);
		ClientAdvisor advisor = clientAdvisorRepository.save(
			new ClientAdvisor(employeeAccount, store, "Advisor Kim")
		);

		var response = authService.loginEmployee(new EmployeeLoginRequest("employee01", "password123!"));

		assertThat(response.role()).isEqualTo(UserRole.CA);
		assertThat(response.caId()).isEqualTo(advisor.getId());
		assertThat(response.storeId()).isEqualTo(store.getId());
		assertThat(response.accessToken()).isNotBlank();
	}

	@Test
	void generatedJwtTokenIsValid() {
		String token = jwtTokenProvider.createAccessToken(AuthenticatedUser.customer(1L, 10L));

		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		AuthenticatedUser principal = (AuthenticatedUser) jwtTokenProvider.getAuthentication(token).getPrincipal();
		assertThat(principal.getRole()).isEqualTo(UserRole.CUSTOMER);
		assertThat(principal.getCustomerId()).isEqualTo(10L);
	}
}
