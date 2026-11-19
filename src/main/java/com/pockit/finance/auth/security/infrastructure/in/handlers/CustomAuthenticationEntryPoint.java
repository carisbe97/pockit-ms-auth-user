package com.pockit.finance.auth.security.infrastructure.in.handlers;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.infrastructure.in.filters.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object attr = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (attr instanceof BaseDomainException domainException) {
            errorResponseWriter.writeError(
                    response,
                    domainException.getStatusCode(),
                    domainException.getCode(),
                    domainException.getMessage()
            );
            return;
        }

        errorResponseWriter.writeError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                BaseConstants.CODE_UNAUTHORIZED,
                BaseConstants.MSG_UNAUTHORIZED
        );
    }
}
