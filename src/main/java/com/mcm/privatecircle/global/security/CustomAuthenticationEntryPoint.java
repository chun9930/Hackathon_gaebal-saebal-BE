package com.mcm.privatecircle.global.security;

import java.io.IOException;

import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.ErrorDetail;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
		throws IOException, ServletException {
		writeError(response, ErrorCode.INVALID_TOKEN);
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new com.fasterxml.jackson.databind.ObjectMapper().writeValue(
			response.getWriter(),
			ApiResponse.fail(new ErrorDetail(errorCode.getCode(), errorCode.getMessage()))
		);
	}
}
