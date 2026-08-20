package com.mcm.privatecircle.global.security;

import java.io.IOException;

import com.mcm.privatecircle.global.exception.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		CustomAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = jwtTokenProvider.resolveToken(request);
		try {
			if (token != null) {
				var authentication = jwtTokenProvider.getAuthentication(token);
				((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) authentication)
					.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				if (request.getRequestURI().contains("/ai-briefs")) {
					log.info("[AI BRIEF] JWT authenticated: method={}, uri={}, authorities={}",
						request.getMethod(), request.getRequestURI(), authentication.getAuthorities());
				}
			}
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			request.setAttribute(
				CustomAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE,
				exception.getErrorCode()
			);
			authenticationEntryPoint.commence(
				request,
				response,
				new AuthenticationServiceException(exception.getMessage(), exception)
			);
		}
	}
}
