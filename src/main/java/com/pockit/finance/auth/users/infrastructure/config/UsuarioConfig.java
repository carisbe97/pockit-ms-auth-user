package com.pockit.finance.auth.users.infrastructure.config;

import com.pockit.finance.auth.users.domain.ports.out.ICategoriaSistemaPort;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import com.pockit.finance.auth.users.domain.services.CreateUsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuracion de wiring de beans del modulo users.
 */
@Configuration
public class UsuarioConfig {

    @Bean
    public CreateUsuarioService createUsuarioService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ICategoriaSistemaPort categoriaSistemaPort,
            IStoragePort storagePort
    ) {
        return new CreateUsuarioService(
                usuarioRepositoryPort,
                passwordHasherPort,
                categoriaSistemaPort,
                storagePort
        );
    }

    @Bean
    public RestClient supabaseRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
