package com.mcm.privatecircle.auth.controller;

import com.mcm.privatecircle.auth.dto.AuthTokenResponse;
import com.mcm.privatecircle.auth.dto.CustomerLoginRequest;
import com.mcm.privatecircle.auth.dto.CustomerSignupRequest;
import com.mcm.privatecircle.auth.dto.EmployeeLoginRequest;
import com.mcm.privatecircle.auth.service.AuthService;
import com.mcm.privatecircle.global.response.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/customers/signup")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(@Valid @RequestBody CustomerSignupRequest request) {
		AuthTokenResponse response = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@PostMapping("/customers/login")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> loginCustomer(@Valid @RequestBody CustomerLoginRequest request) {
		AuthTokenResponse response = authService.loginCustomer(request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/employees/login")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> loginEmployee(@Valid @RequestBody EmployeeLoginRequest request) {
		AuthTokenResponse response = authService.loginEmployee(request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
