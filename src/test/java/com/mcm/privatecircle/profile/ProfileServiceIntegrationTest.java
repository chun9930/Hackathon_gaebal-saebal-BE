package com.mcm.privatecircle.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.auth.dto.CustomerSignupRequest;
import com.mcm.privatecircle.auth.service.AuthService;
import com.mcm.privatecircle.customer.dto.CustomerProfileUpdateRequest;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.customer.service.CustomerService;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.employee.service.EmployeeService;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProfileServiceIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private EmployeeAccountRepository employeeAccountRepository;

	@Autowired
	private ClientAdvisorRepository clientAdvisorRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void customerMyProfileShowsCalculatedFieldsAndCanUpdate() {
		var signup = authService.signup(new CustomerSignupRequest(
			"customer10",
			"password123!",
			"Kim",
			"01010101010"
		));
		AuthenticatedUser customerUser = AuthenticatedUser.customer(signup.accountId(), signup.customerId());

		var profile = customerService.getMyProfile(customerUser);
		assertThat(profile.customerId()).isEqualTo(signup.customerId());
		assertThat(profile.customerNo()).isEqualTo(String.format("C%08d", signup.customerId()));
		assertThat(profile.qrToken()).isNotBlank();
		assertThat(profile.name()).isEqualTo("Kim");
		assertThat(profile.visitCount()).isZero();
		assertThat(profile.stampCount()).isZero();
		assertThat(profile.lastVisitedAt()).isNull();

		var updated = customerService.updateMyProfile(
			customerUser,
			new CustomerProfileUpdateRequest(
				"Kim Updated",
				"01020202020",
				"https://img.example.com/a.png",
				"minimal"
			)
		);

		assertThat(updated.name()).isEqualTo("Kim Updated");
		assertThat(updated.phoneNumber()).isEqualTo("01020202020");
		assertThat(updated.membershipGrade()).isNull();
		assertThat(updated.profileImageUrl()).isEqualTo("https://img.example.com/a.png");
		assertThat(updated.customerNo()).isEqualTo(String.format("C%08d", signup.customerId()));
		assertThat(updated.qrToken()).isNotBlank();

		Customer saved = customerRepository.findById(signup.customerId()).orElseThrow();
		assertThat(saved.getPhoneNumber()).isEqualTo("01020202020");
		assertThat(saved.getMembershipGrade()).isNull();
	}

	@Test
	void customerProfileByQrTokenUsesSameResponseShape() {
		var signup = authService.signup(new CustomerSignupRequest(
			"customer11",
			"password123!",
			"Park",
			"01030303030"
		));

		Customer customer = customerRepository.findById(signup.customerId()).orElseThrow();
		var profile = customerService.getCustomerByQrToken(customer.getQrToken());

		assertThat(profile.customerId()).isEqualTo(signup.customerId());
		assertThat(profile.customerNo()).isEqualTo(String.format("C%08d", signup.customerId()));
		assertThat(profile.qrToken()).isEqualTo(customer.getQrToken());
		assertThat(profile.name()).isEqualTo("Park");
	}

	@Test
	void duplicatePhoneNumberOnCustomerUpdateIsRejected() {
		var first = authService.signup(new CustomerSignupRequest(
			"customer12",
			"password123!",
			"First",
			"01040404040"
		));
		authService.signup(new CustomerSignupRequest(
			"customer13",
			"password123!",
			"Second",
			"01050505050"
		));

		AuthenticatedUser customerUser = AuthenticatedUser.customer(first.accountId(), first.customerId());

		assertThatThrownBy(() -> customerService.updateMyProfile(
			customerUser,
			new CustomerProfileUpdateRequest(
				"First",
				"01050505050",
				null,
				null
			)
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATE_PHONE_NUMBER);
	}

	@Test
	void employeeMyProfileReturnsStoreAndLoginInfo() {
		Store store = storeRepository.save(new Store("Jamsil", "Seoul"));
		EmployeeAccount employeeAccount = employeeAccountRepository.save(
			new EmployeeAccount("employee10", passwordEncoder.encode("password123!"))
		);
		ClientAdvisor advisor = clientAdvisorRepository.save(
			new ClientAdvisor(employeeAccount, store, "Advisor Park")
		);
		AuthenticatedUser caUser = AuthenticatedUser.ca(employeeAccount.getId(), advisor.getId(), store.getId());

		var profile = employeeService.getMyProfile(caUser);

		assertThat(profile.caId()).isEqualTo(advisor.getId());
		assertThat(profile.storeId()).isEqualTo(store.getId());
		assertThat(profile.storeName()).isEqualTo("Jamsil");
		assertThat(profile.loginId()).isEqualTo("employee10");
		assertThat(profile.accountId()).isEqualTo(employeeAccount.getId());
		assertThat(profile.createdAt()).isNotNull();
		assertThat(profile.name()).isEqualTo("Advisor Park");
		assertThat(profile.accountId()).isEqualTo(employeeAccount.getId());
		assertThat(profile.caId()).isNotNull();
		assertThat(profile.accountId()).isNotNull();
		assertThat(profile.storeId()).isNotNull();
		assertThat(profile.caId()).isEqualTo(advisor.getId());
		assertThat(profile.loginId()).contains("employee10");
	}
}
