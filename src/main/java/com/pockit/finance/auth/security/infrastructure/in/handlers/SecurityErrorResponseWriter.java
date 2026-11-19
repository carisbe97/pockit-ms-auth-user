package com.pockit.finance.auth.security.infrastructure.in.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.infrastructure.errors.ErrorResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeError(HttpServletResponse response, int statusCode, String code, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponseDto body = ErrorResponseDto.of(statusCode, code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
