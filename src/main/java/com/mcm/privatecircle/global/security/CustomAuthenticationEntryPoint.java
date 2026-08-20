package com.mcm.privatecircle.global.security;

import java.io.IOException;

import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.ErrorDetail;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

	public static final String ERROR_CODE_ATTRIBUTE =
		CustomAuthenticationEntryPoint.class.getName() + ".ERROR_CODE";

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException, ServletException {
		Object attribute = request.getAttribute(ERROR_CODE_ATTRIBUTE);
		ErrorCode errorCode = attribute instanceof ErrorCode value
			? value
			: ErrorCode.INVALID_TOKEN;
		if (request.getRequestURI().contains("/ai-briefs")) {
			log.warn("[AI BRIEF] Authentication rejected: method={}, uri={}, errorCode={}",
				request.getMethod(), request.getRequestURI(), errorCode.getCode());
		}
		writeError(response, errorCode);
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
