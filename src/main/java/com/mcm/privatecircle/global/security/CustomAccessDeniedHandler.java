package com.mcm.privatecircle.global.security;

import java.io.IOException;

import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.ErrorDetail;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
		throws IOException, ServletException {
		response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new com.fasterxml.jackson.databind.ObjectMapper().writeValue(
			response.getWriter(),
			ApiResponse.fail(new ErrorDetail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()))
		);
	}
}
