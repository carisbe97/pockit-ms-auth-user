package com.pockit.finance.auth.security.infrastructure.in.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.models.EncryptedPayload;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.infrastructure.in.dto.EncryptedPayloadDto;
import com.pockit.finance.auth.security.infrastructure.in.handlers.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadEncryptionFilter extends OncePerRequestFilter {

    private final ICryptoPort cryptoPort;
    private final ObjectMapper objectMapper;
    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String encryptedHeader = request.getHeader(BaseConstants.HEADER_PAYLOAD_ENCRYPTED);
        if (!"true".equalsIgnoreCase(encryptedHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            byte[] rawBody = request.getInputStream().readAllBytes();
            if (rawBody.length == 0) {
                throw new BaseDomainException(
                        400,
                        BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                        BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
                );
            }

            EncryptedPayloadDto dto = objectMapper.readValue(
                    new String(rawBody, StandardCharsets.UTF_8),
                    EncryptedPayloadDto.class
            );

            if (dto.timestamp() == null) {
                throw new BaseDomainException(
                        400,
                        BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                        BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
                );
            }

            EncryptedPayload payload = new EncryptedPayload(
                    dto.encryptedKey(),
                    dto.iv(),
                    dto.cipherText(),
                    dto.timestamp()
            );

            String plainJson = cryptoPort.decryptPayload(payload);
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, plainJson);
            filterChain.doFilter(wrappedRequest, response);

        } catch (BaseDomainException ex) {
            log.warn("Payload cifrado rechazado [{}]: {}", ex.getCode(), ex.getMessage());
            errorResponseWriter.writeError(response, ex.getStatusCode(), ex.getCode(), ex.getMessage());
        } catch (JsonProcessingException ex) {
            log.warn("Sobre JSON cifrado invalido: {}", ex.getMessage());
            errorResponseWriter.writeError(
                    response,
                    400,
                    BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                    BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
            );
        }
    }
}
