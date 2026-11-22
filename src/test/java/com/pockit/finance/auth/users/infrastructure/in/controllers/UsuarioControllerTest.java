package com.pockit.finance.auth.users.infrastructure.in.controllers;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.errors.BaseControllerAdvice;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.security.infrastructure.config.SecurityConfig;
import com.pockit.finance.auth.security.infrastructure.config.WebMvcConfig;
import com.pockit.finance.auth.security.infrastructure.in.filters.JwtAuthenticationFilter;
import com.pockit.finance.auth.security.infrastructure.in.filters.PayloadEncryptionFilter;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAccessDeniedHandler;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAuthenticationEntryPoint;
import com.pockit.finance.auth.security.infrastructure.in.handlers.SecurityErrorResponseWriter;
import com.pockit.finance.auth.security.infrastructure.in.resolvers.CurrentUserIdArgumentResolver;
import com.pockit.finance.auth.security.infrastructure.out.jwt.JwtTokenAdapter;
import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IGetUsuarioPerfilUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IUpdateAvatarUseCase;
import com.pockit.finance.auth.users.infrastructure.config.in.mappers.IUsuarioWebMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfig.class,
        BaseControllerAdvice.class,
        IUsuarioWebMapperImpl.class,
        JwtTokenAdapter.class,
        PayloadEncryptionFilter.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CurrentUserIdArgumentResolver.class
})
@TestPropertySource(properties = {
        "app.security.jwt.secret=pockit-test-jwt-secret-key-minimo-64-bytes-para-hs512-0123456789abcdef",
        "app.security.jwt.issuer=pockit-ms-auth-user",
        "app.security.jwt.expiration-seconds=900",
        "app.crypto.replay-window-seconds=300",
        "app.security.cors.allowed-origins=http://localhost:4200"
})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ITokenPort tokenPort;

    @MockBean
    private IGetUsuarioPerfilUseCase getUsuarioPerfilUseCase;

    @MockBean
    private IUpdateAvatarUseCase updateAvatarUseCase;

    @MockBean
    private ICryptoPort cryptoPort;

    @Test
    @DisplayName("GET /api/usuarios/me debe retornar 200 OK con el perfil del usuario autenticado con JWT real")
    void shouldReturnProfileWithValidJwt() throws Exception {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$secrethashshouldneverappear")
                .usrNombre("Camilo")
                .usrApellido("Perez")
                .usrEdad((short) 28)
                .usrActivo(true)
                .usrCreadoEn(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        String jwt = tokenPort.generarAccessToken(usuario);
        when(getUsuarioPerfilUseCase.ejecutar(usrId)).thenReturn(usuario);

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usrId").value(usrId.toString()))
                .andExpect(jsonPath("$.usrCorreo").value("camilo@pockit.com"))
                .andExpect(jsonPath("$.usrNombre").value("Camilo"))
                .andExpect(content().string(not(containsString("usrPasswordHash"))))
                .andExpect(content().string(not(containsString("secrethashshouldneverappear"))));
    }

    @Test
    @DisplayName("GET /api/usuarios/me debe retornar 401 Unauthorized cuando no se envia JWT")
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_UNAUTHORIZED));
    }

    @Test
    @DisplayName("GET /api/usuarios/me debe retornar 404 Not Found cuando el usuario del JWT no existe")
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        UUID usrId = UUID.randomUUID();
        Usuario tokenUser = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("ghost@pockit.com")
                .build();
        String jwt = tokenPort.generarAccessToken(tokenUser);

        when(getUsuarioPerfilUseCase.ejecutar(usrId))
                .thenThrow(new BaseDomainException(404, BaseConstants.CODE_USUARIO_NO_ENCONTRADO, BaseConstants.MSG_USUARIO_NO_ENCONTRADO));

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_USUARIO_NO_ENCONTRADO));
    }

    @Test
    @DisplayName("PUT /api/usuarios/me/avatar debe retornar 200 OK con el perfil actualizado cuando la imagen es valida")
    void shouldUpdateAvatarWithValidJwtAndImage() throws Exception {
        UUID usrId = UUID.randomUUID();
        String newAvatarUrl = "https://supabase.test/storage/v1/object/public/avatars/" + usrId + "/avatar.png";
        Usuario actualizado = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$secrethash")
                .usrNombre("Camilo")
                .usrApellido("Perez")
                .usrEdad((short) 28)
                .usrAvatarUrl(newAvatarUrl)
                .usrActivo(true)
                .usrCreadoEn(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        String jwt = tokenPort.generarAccessToken(actualizado);
        when(updateAvatarUseCase.ejecutar(any(UpdateAvatarCommand.class))).thenReturn(actualizado);

        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        MockMultipartFile avatarPart = new MockMultipartFile(
                "avatar", "avatar.png", MediaType.IMAGE_PNG_VALUE, baos.toByteArray()
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/avatar")
                        .file(avatarPart)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usrId").value(usrId.toString()))
                .andExpect(jsonPath("$.usrAvatarUrl").value(newAvatarUrl))
                .andExpect(content().string(not(containsString("usrPasswordHash"))));
    }

    @Test
    @DisplayName("PUT /api/usuarios/me/avatar debe retornar 401 Unauthorized sin JWT")
    void shouldReturn401WhenUpdatingAvatarWithoutJwt() throws Exception {
        MockMultipartFile avatarPart = new MockMultipartFile(
                "avatar", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/avatar")
                        .file(avatarPart))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_UNAUTHORIZED));
    }

    @Test
    @DisplayName("PUT /api/usuarios/me/avatar debe retornar 400 IMAGEN_VACIA cuando no se envia archivo")
    void shouldReturn400WhenAvatarIsMissing() throws Exception {
        UUID usrId = UUID.randomUUID();
        String jwt = tokenPort.generarAccessToken(Usuario.builder().usrId(usrId).usrCorreo("camilo@pockit.com").build());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/avatar")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_IMAGEN_VACIA));
    }

    @Test
    @DisplayName("PUT /api/usuarios/me/avatar debe retornar 413 cuando el avatar excede 2MB")
    void shouldReturn413WhenAvatarExceedsMaxSize() throws Exception {
        UUID usrId = UUID.randomUUID();
        String jwt = tokenPort.generarAccessToken(Usuario.builder().usrId(usrId).usrCorreo("camilo@pockit.com").build());

        MockMultipartFile oversized = new MockMultipartFile(
                "avatar", "big.png", MediaType.IMAGE_PNG_VALUE,
                new byte[BaseConstants.MAX_IMAGE_SIZE_BYTES + 1]
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/avatar")
                        .file(oversized)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.statusCode").value(413))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO));
    }
}
