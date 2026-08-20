package com.mcm.privatecircle.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerHttpTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customerSignupIsPublicAndReturnsCreatedToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/customers/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "httpCustomer01",
					  "password": "password123!",
					  "name": "HTTP Customer",
					  "phoneNumber": "01077000001"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.role").value("CUSTOMER"))
			.andExpect(jsonPath("$.data.customerId").isNumber());
	}

	@Test
	void duplicateCustomerSignupReturnsConflictCode() throws Exception {
		String firstPayload = """
			{
			  "loginId": "httpCustomer02",
			  "password": "password123!",
			  "name": "First Customer",
			  "phoneNumber": "01077000002"
			}
			""";
		mockMvc.perform(post("/api/v1/auth/customers/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(firstPayload))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/customers/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "httpCustomer02",
					  "password": "password123!",
					  "name": "Duplicate Customer",
					  "phoneNumber": "01077000003"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_LOGIN_ID"));
	}
}