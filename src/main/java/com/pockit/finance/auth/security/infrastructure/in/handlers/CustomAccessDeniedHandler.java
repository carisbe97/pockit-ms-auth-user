package com.pockit.finance.auth.security.infrastructure.in.handlers;

import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        errorResponseWriter.writeError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                BaseConstants.CODE_FORBIDDEN,
                BaseConstants.MSG_FORBIDDEN
        );
    }
}
