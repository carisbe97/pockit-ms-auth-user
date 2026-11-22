package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.services.CreateUsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalCreateUsuarioAdapterTest {

    @Mock
    private CreateUsuarioService createUsuarioService;

    @InjectMocks
    private TransactionalCreateUsuarioAdapter adapter;

    @Test
    @DisplayName("Debe delegar la ejecucion al servicio de dominio")
    void shouldDelegateToDomainService() {
        CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                "user@pockit.com", "Pockit@2026", "User", "Test", (short) 22, null, null, null
        );
        Usuario expected = Usuario.builder().usrId(UUID.randomUUID()).usrCorreo("user@pockit.com").build();
        when(createUsuarioService.ejecutar(cmd)).thenReturn(expected);

        Usuario actual = adapter.ejecutar(cmd);

        assertThat(actual).isSameAs(expected);
        verify(createUsuarioService).ejecutar(cmd);
    }
}
