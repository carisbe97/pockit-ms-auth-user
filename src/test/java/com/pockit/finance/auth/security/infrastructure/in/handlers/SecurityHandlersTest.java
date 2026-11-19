package com.pockit.finance.auth.security.infrastructure.in.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.infrastructure.in.filters.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHandlersTest {

    private CustomAuthenticationEntryPoint entryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(new ObjectMapper());
        entryPoint = new CustomAuthenticationEntryPoint(writer);
        accessDeniedHandler = new CustomAccessDeniedHandler(writer);
    }

    @Test
    @DisplayName("CustomAuthenticationEntryPoint debe responder 401 uniforme con o sin AUTH_ERROR_ATTRIBUTE")
    void shouldWrite401InUniformFormat() throws Exception {
        // Sin atributo -> NO_AUTENTICADO
        MockHttpServletRequest reqNoAttr = new MockHttpServletRequest();
        MockHttpServletResponse resNoAttr = new MockHttpServletResponse();
        entryPoint.commence(reqNoAttr, resNoAttr, new InsufficientAuthenticationException("No auth"));
        assertThat(resNoAttr.getStatus()).isEqualTo(401);
        assertThat(resNoAttr.getContentAsString()).contains(BaseConstants.CODE_UNAUTHORIZED);

        // Con atributo -> TOKEN_EXPIRADO
        MockHttpServletRequest reqAttr = new MockHttpServletRequest();
        reqAttr.setAttribute(
                JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE,
                new BaseDomainException(401, BaseConstants.CODE_TOKEN_EXPIRADO, BaseConstants.MSG_TOKEN_EXPIRADO)
        );
        MockHttpServletResponse resAttr = new MockHttpServletResponse();
        entryPoint.commence(reqAttr, resAttr, new InsufficientAuthenticationException("Expired"));
        assertThat(resAttr.getStatus()).isEqualTo(401);
        assertThat(resAttr.getContentAsString()).contains(BaseConstants.CODE_TOKEN_EXPIRADO);
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler debe responder 403 uniforme")
    void shouldWrite403InUniformFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(BaseConstants.CODE_FORBIDDEN);
    }
}
