package com.cursed.auth.exceptions.handlers;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        AccessDeniedException accessDeniedException)
                        throws IOException, ServletException {

                ErrorDTO error = ErrorDTO.builder()
                                .code("FORBIDDEN")
                                .message("You do not have permission to access this resource")
                                .details(accessDeniedException.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build();

                BaseResponseDTO body = BaseResponseDTO.builder()
                                .success(false)
                                .error(error)
                                .build();

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), body);
        }
}
