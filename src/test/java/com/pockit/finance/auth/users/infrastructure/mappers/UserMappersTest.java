package com.pockit.finance.auth.users.infrastructure.mappers;

import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.UsuarioResponseDto;
import com.pockit.finance.auth.users.infrastructure.config.in.mappers.IUsuarioWebMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.SesionRefreshEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.UsuarioEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.ISesionRefreshPersistenceMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.IUsuarioPersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMappersTest {

    private final IUsuarioWebMapper webMapper = Mappers.getMapper(IUsuarioWebMapper.class);
    private final IUsuarioPersistenceMapper usuarioPersistenceMapper = Mappers.getMapper(IUsuarioPersistenceMapper.class);
    private final ISesionRefreshPersistenceMapper sesionPersistenceMapper = Mappers.getMapper(ISesionRefreshPersistenceMapper.class);

    @Test
    @DisplayName("MapStruct debe mapear Usuario <-> UsuarioEntity y Usuario -> UsuarioResponseDto con Lombok")
    void shouldMapUsuarioBidirectionallyAndToResponseDto() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Usuario domain = Usuario.builder()
                .usrId(id)
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$secretHash")
                .usrNombre("Camilo")
                .usrApellido("Aristizabal")
                .usrEdad((short) 28)
                .usrAvatarUrl("https://supabase.co/pockit-avatars/camilo.png")
                .usrActivo(true)
                .usrResetTokenHash("reset-hash")
                .usrResetExpiraEn(now.plusMinutes(15))
                .usrCreadoEn(now)
                .usrActualizadoEn(now)
                .build();

        UsuarioEntity entity = usuarioPersistenceMapper.toEntity(domain);
        assertThat(entity.getUsrId()).isEqualTo(id);
        assertThat(entity.getUsrCorreo()).isEqualTo("camilo@pockit.com");
        assertThat(entity.getUsrPasswordHash()).isEqualTo("$2a$12$secretHash");
        assertThat(entity.getUsrEdad()).isEqualTo((short) 28);

        Usuario backToDomain = usuarioPersistenceMapper.toDomain(entity);
        assertThat(backToDomain.getUsrId()).isEqualTo(id);
        assertThat(backToDomain.getUsrAvatarUrl()).isEqualTo(domain.getUsrAvatarUrl());

        UsuarioResponseDto responseDto = webMapper.toResponseDto(backToDomain);
        assertThat(responseDto.usrId()).isEqualTo(id);
        assertThat(responseDto.usrNombre()).isEqualTo("Camilo");
        assertThat(responseDto.usrApellido()).isEqualTo("Aristizabal");
        assertThat(responseDto.usrEdad()).isEqualTo((short) 28);

        assertThat(webMapper.toResponseDto(null)).isNull();
        assertThat(usuarioPersistenceMapper.toEntity(null)).isNull();
        assertThat(usuarioPersistenceMapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("MapStruct debe mapear SesionRefresh <-> SesionRefreshEntity correctamente")
    void shouldMapSesionRefreshBidirectionally() {
        UUID usrId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SesionRefresh domain = SesionRefresh.builder()
                .srfId(42L)
                .srfUsrId(usrId)
                .srfTokenHash("sha256-hash")
                .srfExpiraEn(now.plusDays(7))
                .srfRevocado(false)
                .srfDispositivo("iOS 17")
                .srfCreadoEn(now)
                .build();

        SesionRefreshEntity entity = sesionPersistenceMapper.toEntity(domain);
        assertThat(entity.getSrfId()).isEqualTo(42L);
        assertThat(entity.getSrfUsrId()).isEqualTo(usrId);
        assertThat(entity.getSrfTokenHash()).isEqualTo("sha256-hash");

        SesionRefresh backToDomain = sesionPersistenceMapper.toDomain(entity);
        assertThat(backToDomain.getSrfId()).isEqualTo(42L);
        assertThat(backToDomain.getSrfDispositivo()).isEqualTo("iOS 17");

        assertThat(sesionPersistenceMapper.toEntity(null)).isNull();
        assertThat(sesionPersistenceMapper.toDomain(null)).isNull();
    }
}
