package com.pockit.finance.auth.security.infrastructure.in.controllers;

import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityKeyControllerTest {

    @Mock
    private ICryptoPort cryptoPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityKeyController controller = new SecurityKeyController(cryptoPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/security/public-key debe retornar algoritmo y llave publica RSA con Cache-Control no-store")
    void shouldReturnPublicKey() throws Exception {
        when(cryptoPort.getAlgorithm()).thenReturn(BaseConstants.RSA_OAEP_ALGORITHM);
        when(cryptoPort.getPublicKeyBase64()).thenReturn("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A");

        mockMvc.perform(get("/api/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.algorithm").value(BaseConstants.RSA_OAEP_ALGORITHM))
                .andExpect(jsonPath("$.publicKey").value("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A"));
    }
}
