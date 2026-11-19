package com.pockit.finance.auth.security.infrastructure.in.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.models.EncryptedPayload;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.infrastructure.in.handlers.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayloadEncryptionFilterTest {

    @Mock
    private ICryptoPort cryptoPort;

    @Mock
    private FilterChain filterChain;

    private PayloadEncryptionFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SecurityErrorResponseWriter errorWriter = new SecurityErrorResponseWriter(objectMapper);
        filter = new PayloadEncryptionFilter(cryptoPort, objectMapper, errorWriter);
    }

    @Test
    @DisplayName("Debe dejar pasar peticiones sin header X-Payload-Encrypted sin tocar el body")
    void shouldPassThroughWithoutHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{\"plain\":true}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(cryptoPort, never()).decryptPayload(any());
    }

    @Test
    @DisplayName("Debe descifrar payload cuando X-Payload-Encrypted=true y exponer JSON limpio en CachedBodyHttpServletRequest")
    void shouldDecryptAndWrapRequest() throws Exception {
        String envelopeJson = "{\"encryptedKey\":\"k\",\"iv\":\"i\",\"cipherText\":\"c\",\"timestamp\":1763487000}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true");
        request.setContent(envelopeJson.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        String plainJson = "{\"correo\":\"camilo@pockit.com\"}";
        when(cryptoPort.decryptPayload(any(EncryptedPayload.class))).thenReturn(plainJson);

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<ServletRequest> reqCaptor = ArgumentCaptor.forClass(ServletRequest.class);
        verify(filterChain).doFilter(reqCaptor.capture(), any());
        assertThat(reqCaptor.getValue()).isInstanceOf(CachedBodyHttpServletRequest.class);

        CachedBodyHttpServletRequest wrapped = (CachedBodyHttpServletRequest) reqCaptor.getValue();
        assertThat(wrapped.getReader().readLine()).isEqualTo(plainJson);
        assertThat(wrapped.getContentLength()).isEqualTo(plainJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(wrapped.getHeader("Content-Length")).isEqualTo(String.valueOf(plainJson.getBytes(StandardCharsets.UTF_8).length));
        assertThat(wrapped.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    @DisplayName("Debe responder 400 con ErrorResponseDto si el sobre esta vacio, mal formado o falla descifrado")
    void shouldReturn400WhenEncryptedPayloadFails() throws Exception {
        // Caso 1: body vacio
        MockHttpServletRequest emptyReq = new MockHttpServletRequest();
        emptyReq.addHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true");
        MockHttpServletResponse emptyRes = new MockHttpServletResponse();
        filter.doFilter(emptyReq, emptyRes, filterChain);
        assertThat(emptyRes.getStatus()).isEqualTo(400);
        assertThat(emptyRes.getContentAsString()).contains(BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA);

        // Caso 2: timestamp null
        MockHttpServletRequest noTsReq = new MockHttpServletRequest();
        noTsReq.addHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true");
        noTsReq.setContent("{\"encryptedKey\":\"k\",\"iv\":\"i\",\"cipherText\":\"c\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse noTsRes = new MockHttpServletResponse();
        filter.doFilter(noTsReq, noTsRes, filterChain);
        assertThat(noTsRes.getStatus()).isEqualTo(400);

        // Caso 3: JSON roto
        MockHttpServletRequest badJsonReq = new MockHttpServletRequest();
        badJsonReq.addHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true");
        badJsonReq.setContent("{broken".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse badJsonRes = new MockHttpServletResponse();
        filter.doFilter(badJsonReq, badJsonRes, filterChain);
        assertThat(badJsonRes.getStatus()).isEqualTo(400);

        // Caso 4: cryptoPort lanza PAYLOAD_EXPIRADO
        MockHttpServletRequest expReq = new MockHttpServletRequest();
        expReq.addHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true");
        expReq.setContent("{\"encryptedKey\":\"k\",\"iv\":\"i\",\"cipherText\":\"c\",\"timestamp\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse expRes = new MockHttpServletResponse();
        when(cryptoPort.decryptPayload(any())).thenThrow(
                new BaseDomainException(400, BaseConstants.CODE_PAYLOAD_EXPIRADO, BaseConstants.MSG_PAYLOAD_EXPIRADO)
        );
        filter.doFilter(expReq, expRes, filterChain);
        assertThat(expRes.getStatus()).isEqualTo(400);
        assertThat(expRes.getContentAsString()).contains(BaseConstants.CODE_PAYLOAD_EXPIRADO);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
