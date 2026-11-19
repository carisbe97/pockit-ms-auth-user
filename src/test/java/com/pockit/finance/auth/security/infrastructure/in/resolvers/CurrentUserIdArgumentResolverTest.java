package com.pockit.finance.auth.security.infrastructure.in.resolvers;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.infrastructure.in.annotations.CurrentUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserIdArgumentResolverTest {

    private CurrentUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        resolver = new CurrentUserIdArgumentResolver();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("supportsParameter debe aceptar solo UUID anotado con @CurrentUserId")
    void shouldSupportAnnotatedUuidOnly() throws Exception {
        Method method = DummyMethods.class.getDeclaredMethod("sample", UUID.class, String.class, UUID.class);
        MethodParameter validParam = new MethodParameter(method, 0);
        MethodParameter wrongTypeParam = new MethodParameter(method, 1);
        MethodParameter unannotatedParam = new MethodParameter(method, 2);

        assertThat(resolver.supportsParameter(validParam)).isTrue();
        assertThat(resolver.supportsParameter(wrongTypeParam)).isFalse();
        assertThat(resolver.supportsParameter(unannotatedParam)).isFalse();
    }

    @Test
    @DisplayName("resolveArgument debe retornar el UUID autenticado o lanzar 401 si no hay sesion valida")
    void shouldResolveUuidOrThrow401() throws Exception {
        Method method = DummyMethods.class.getDeclaredMethod("sample", UUID.class, String.class, UUID.class);
        MethodParameter validParam = new MethodParameter(method, 0);
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(validParam, null, webRequest, null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode())
                        .isEqualTo(BaseConstants.CODE_UNAUTHORIZED));

        UUID expectedId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(expectedId, null, List.of())
        );

        Object resolved = resolver.resolveArgument(validParam, null, webRequest, null);
        assertThat(resolved).isEqualTo(expectedId);
    }

    static class DummyMethods {
        void sample(@CurrentUserId UUID userId, @CurrentUserId String wrongType, UUID plainUuid) {
        }
    }
}
