package com.pockit.finance.auth.users.infrastructure.out.persistence.adapters;

import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.SesionRefreshEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.UsuarioEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.ISesionRefreshPersistenceMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.IUsuarioPersistenceMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.repositories.ISesionRefreshJpaRepository;
import com.pockit.finance.auth.users.infrastructure.out.persistence.repositories.IUsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceAdaptersTest {

    @Mock
    private IUsuarioJpaRepository usuarioJpaRepository;

    @Mock
    private ISesionRefreshJpaRepository sesionRefreshJpaRepository;

    private UsuarioPersistenceAdapter usuarioAdapter;
    private SesionRefreshPersistenceAdapter sesionAdapter;

    @BeforeEach
    void setUp() {
        IUsuarioPersistenceMapper usuarioMapper = Mappers.getMapper(IUsuarioPersistenceMapper.class);
        ISesionRefreshPersistenceMapper sesionMapper = Mappers.getMapper(ISesionRefreshPersistenceMapper.class);
        usuarioAdapter = new UsuarioPersistenceAdapter(usuarioJpaRepository, usuarioMapper);
        sesionAdapter = new SesionRefreshPersistenceAdapter(sesionRefreshJpaRepository, sesionMapper);
    }

    @Test
    @DisplayName("UsuarioPersistenceAdapter debe delegar save, findById, findByCorreo y existsByCorreo")
    void shouldDelegateUsuarioRepositoryOperations() {
        UUID id = UUID.randomUUID();
        Usuario usuario = Usuario.builder().usrId(id).usrCorreo("test@pockit.com").usrNombre("Test").build();
        UsuarioEntity entity = UsuarioEntity.builder().usrId(id).usrCorreo("test@pockit.com").usrNombre("Test").build();

        when(usuarioJpaRepository.save(any(UsuarioEntity.class))).thenReturn(entity);
        when(usuarioJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(usuarioJpaRepository.findByUsrCorreoIgnoreCase("test@pockit.com")).thenReturn(Optional.of(entity));
        when(usuarioJpaRepository.existsByUsrCorreoIgnoreCase("test@pockit.com")).thenReturn(true);

        assertThat(usuarioAdapter.save(usuario).getUsrId()).isEqualTo(id);
        assertThat(usuarioAdapter.findById(id)).isPresent();
        assertThat(usuarioAdapter.findByCorreo("test@pockit.com")).isPresent();
        assertThat(usuarioAdapter.existsByCorreo("test@pockit.com")).isTrue();
    }

    @Test
    @DisplayName("SesionRefreshPersistenceAdapter debe delegar save, findByTokenHash y revocarTodasPorUsuario")
    void shouldDelegateSesionRefreshRepositoryOperations() {
        UUID usrId = UUID.randomUUID();
        OffsetDateTime expira = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        SesionRefresh sesion = SesionRefresh.builder()
                .srfId(10L)
                .srfUsrId(usrId)
                .srfTokenHash("hash-abc")
                .srfExpiraEn(expira)
                .srfRevocado(false)
                .build();
        SesionRefreshEntity entity = SesionRefreshEntity.builder()
                .srfId(10L)
                .srfUsrId(usrId)
                .srfTokenHash("hash-abc")
                .srfExpiraEn(expira)
                .srfRevocado(false)
                .build();

        when(sesionRefreshJpaRepository.save(any(SesionRefreshEntity.class))).thenReturn(entity);
        when(sesionRefreshJpaRepository.findBySrfTokenHash("hash-abc")).thenReturn(Optional.of(entity));

        assertThat(sesionAdapter.save(sesion).getSrfId()).isEqualTo(10L);
        assertThat(sesionAdapter.findByTokenHash("hash-abc")).isPresent();

        sesionAdapter.revocarTodasPorUsuario(usrId);
        verify(sesionRefreshJpaRepository).revocarTodasPorUsuario(usrId);
    }
}
