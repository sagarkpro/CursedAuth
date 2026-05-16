package com.cursed.auth.exceptions.handlers;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException)
			throws IOException, ServletException {

		ErrorDTO error = ErrorDTO.builder()
				.code("UNAUTHORIZED")
				.message("Authentication required")
				.details(authException.getMessage())
				.timestamp(LocalDateTime.now())
				.build();

		BaseResponseDTO body = BaseResponseDTO.builder()
				.success(false)
				.error(error)
				.build();

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
