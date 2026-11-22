package com.pockit.finance.auth.users.infrastructure.in.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.errors.BaseControllerAdvice;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.security.infrastructure.config.SecurityConfig;
import com.pockit.finance.auth.security.infrastructure.in.filters.JwtAuthenticationFilter;
import com.pockit.finance.auth.security.infrastructure.in.filters.PayloadEncryptionFilter;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAccessDeniedHandler;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAuthenticationEntryPoint;
import com.pockit.finance.auth.security.infrastructure.in.handlers.SecurityErrorResponseWriter;
import com.pockit.finance.auth.users.domain.exceptions.RefreshTokenReuseException;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.models.RecuperarPasswordCommand;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.models.RestablecerPasswordCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.ICreateUsuarioUseCase;
import com.pockit.finance.auth.users.domain.ports.in.ILoginUseCase;
import com.pockit.finance.auth.users.domain.ports.in.ILogoutUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IRecuperarPasswordUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IRefreshTokenUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IRestablecerPasswordUseCase;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.CreateUsuarioDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.LoginRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.LogoutRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.RecuperarPasswordRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.RefreshTokenRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.RestablecerPasswordRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.mappers.IUsuarioWebMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        BaseControllerAdvice.class,
        IUsuarioWebMapperImpl.class,
        PayloadEncryptionFilter.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ICreateUsuarioUseCase createUsuarioUseCase;

    @MockBean
    private ILoginUseCase loginUseCase;

    @MockBean
    private IRefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private ILogoutUseCase logoutUseCase;

    @MockBean
    private IRecuperarPasswordUseCase recuperarPasswordUseCase;

    @MockBean
    private IRestablecerPasswordUseCase restablecerPasswordUseCase;

    @MockBean
    private ICryptoPort cryptoPort;

    @MockBean
    private ITokenPort tokenPort;

    @Test
    @DisplayName("POST /api/auth/registro (JSON) debe retornar 201 Created sin exponer usrPasswordHash")
    void shouldRegisterUserWithJsonAndReturn201() throws Exception {
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

        when(createUsuarioUseCase.ejecutar(any(CreateUsuarioCommand.class))).thenReturn(usuario);

        CreateUsuarioDto dto = new CreateUsuarioDto(
                "camilo@pockit.com", "Pockit@2026", "Camilo", "Perez", (short) 28
        );

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usrId").value(usrId.toString()))
                .andExpect(jsonPath("$.usrCorreo").value("camilo@pockit.com"))
                .andExpect(jsonPath("$.usrNombre").value("Camilo"))
                .andExpect(jsonPath("$.usrPasswordHash").doesNotExist())
                .andExpect(content().string(not(containsString("usrPasswordHash"))))
                .andExpect(content().string(not(containsString("secrethashshouldneverappear"))));
    }

    @Test
    @DisplayName("POST /api/auth/registro (Multipart con avatar valido) debe retornar 201 Created con usrAvatarUrl")
    void shouldRegisterUserWithMultipartAndAvatar() throws Exception {
        UUID usrId = UUID.randomUUID();
        String avatarUrl = "https://supabase.test/storage/v1/object/public/avatars/" + usrId + "/avatar.png";
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("ana@pockit.com")
                .usrPasswordHash("$2a$12$secrethash")
                .usrNombre("Ana")
                .usrApellido("Gomez")
                .usrEdad((short) 25)
                .usrAvatarUrl(avatarUrl)
                .usrActivo(true)
                .usrCreadoEn(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(createUsuarioUseCase.ejecutar(any(CreateUsuarioCommand.class))).thenReturn(usuario);

        CreateUsuarioDto dto = new CreateUsuarioDto(
                "ana@pockit.com", "Pockit@2026", "Ana", "Gomez", (short) 25
        );
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8)
        );

        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        MockMultipartFile avatarPart = new MockMultipartFile(
                "avatar", "avatar.png", MediaType.IMAGE_PNG_VALUE, baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/auth/registro")
                        .file(datosPart)
                        .file(avatarPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usrId").value(usrId.toString()))
                .andExpect(jsonPath("$.usrAvatarUrl").value(avatarUrl))
                .andExpect(content().string(not(containsString("usrPasswordHash"))));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 400 cuando faltan datos obligatorios o el DTO es invalido")
    void shouldReturn400WhenValidationFails() throws Exception {
        CreateUsuarioDto invalidDto = new CreateUsuarioDto(
                "correo-invalido", "123", "", "", (short) -5
        );

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_VALIDATION_ERROR));

        // Falta parte 'datos' en multipart -> 400
        mockMvc.perform(multipart("/api/auth/registro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_MISSING_PARAMETER));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 409 cuando el correo ya esta registrado")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(createUsuarioUseCase.ejecutar(any(CreateUsuarioCommand.class)))
                .thenThrow(new BaseDomainException(409, BaseConstants.CODE_CORREO_YA_REGISTRADO, "Correo ya registrado"));

        CreateUsuarioDto dto = new CreateUsuarioDto(
                "dup@pockit.com", "Pockit@2026", "Juan", "Dia", (short) 30
        );

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_CORREO_YA_REGISTRADO));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 413 cuando el avatar excede 2MB")
    void shouldReturn413WhenAvatarExceedsMaxSize() throws Exception {
        CreateUsuarioDto dto = new CreateUsuarioDto(
                "big@pockit.com", "Pockit@2026", "Luis", "Mora", (short) 30
        );
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile oversizedAvatar = new MockMultipartFile(
                "avatar", "big.png", MediaType.IMAGE_PNG_VALUE,
                new byte[BaseConstants.MAX_IMAGE_SIZE_BYTES + 1]
        );

        mockMvc.perform(multipart("/api/auth/registro")
                        .file(datosPart)
                        .file(oversizedAvatar))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.statusCode").value(413))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 422 cuando falla regla de dominio (ej. password debil)")
    void shouldReturn422WhenDomainRuleFails() throws Exception {
        when(createUsuarioUseCase.ejecutar(any(CreateUsuarioCommand.class)))
                .thenThrow(new BaseDomainException(422, BaseConstants.CODE_PASSWORD_DEBIL, "Contrasena debil"));

        CreateUsuarioDto dto = new CreateUsuarioDto(
                "weak@pockit.com", "abcdefgh", "Luis", "Mora", (short) 30
        );

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_PASSWORD_DEBIL));
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 200 OK con tokens y perfil sin usrPasswordHash")
    void shouldLoginAndReturn200() throws Exception {
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
        AuthResult authResult = new AuthResult("jwt-token", "opaque-refresh", "Bearer", 900L, usuario);

        when(loginUseCase.ejecutar(any(LoginCommand.class))).thenReturn(authResult);

        LoginRequestDto dto = new LoginRequestDto("camilo@pockit.com", "Pockit@2026", "Web");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("opaque-refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.usuario.usrId").value(usrId.toString()))
                .andExpect(jsonPath("$.usuario.usrCorreo").value("camilo@pockit.com"))
                .andExpect(content().string(not(containsString("usrPasswordHash"))))
                .andExpect(content().string(not(containsString("secrethashshouldneverappear"))));
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 401 cuando las credenciales son invalidas")
    void shouldReturn401WhenLoginCredentialsAreInvalid() throws Exception {
        when(loginUseCase.ejecutar(any(LoginCommand.class)))
                .thenThrow(new BaseDomainException(401, BaseConstants.CODE_CREDENCIALES_INVALIDAS, BaseConstants.MSG_CREDENCIALES_INVALIDAS));

        LoginRequestDto dto = new LoginRequestDto("camilo@pockit.com", "WrongPass", null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_CREDENCIALES_INVALIDAS));
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 400 cuando el DTO es invalido")
    void shouldReturn400WhenLoginValidationFails() throws Exception {
        LoginRequestDto invalidDto = new LoginRequestDto("no-es-correo", "", null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_VALIDATION_ERROR));
    }

    @Test
    @DisplayName("POST /api/auth/refresh debe retornar 200 OK con nuevo par de tokens")
    void shouldRefreshAndReturn200() throws Exception {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrNombre("Camilo")
                .usrActivo(true)
                .build();
        AuthResult authResult = new AuthResult("new-jwt", "new-refresh", "Bearer", 900L, usuario);

        when(refreshTokenUseCase.ejecutar(any(RefreshTokenCommand.class))).thenReturn(authResult);

        RefreshTokenRequestDto dto = new RefreshTokenRequestDto("old-refresh", "Mobile");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.usrId").value(usrId.toString()));
    }

    @Test
    @DisplayName("POST /api/auth/refresh debe retornar 401 cuando el refresh token es invalido o reutilizado")
    void shouldReturn401WhenRefreshTokenIsInvalidOrReused() throws Exception {
        when(refreshTokenUseCase.ejecutar(any(RefreshTokenCommand.class)))
                .thenThrow(new RefreshTokenReuseException("Reuso detectado"));

        RefreshTokenRequestDto dto = new RefreshTokenRequestDto("reused-token", null);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_REFRESH_TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("POST /api/auth/logout debe retornar 204 No Content cuando el token es valido")
    void shouldLogoutAndReturn204() throws Exception {
        LogoutRequestDto dto = new LogoutRequestDto("valid-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(logoutUseCase).ejecutar(new LogoutCommand("valid-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/logout debe retornar 400 cuando falta el refreshToken")
    void shouldReturn400WhenLogoutRefreshTokenIsBlank() throws Exception {
        LogoutRequestDto invalidDto = new LogoutRequestDto("");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_VALIDATION_ERROR));
    }

    @Test
    @DisplayName("POST /api/auth/recuperar-password debe retornar 200 OK con mensaje generico")
    void shouldRecoverPasswordAndReturn200() throws Exception {
        RecuperarPasswordRequestDto dto = new RecuperarPasswordRequestDto("camilo@pockit.com");

        mockMvc.perform(post("/api/auth/recuperar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(BaseConstants.MSG_RECUPERAR_PASSWORD_OK));

        org.mockito.Mockito.verify(recuperarPasswordUseCase).ejecutar(new RecuperarPasswordCommand("camilo@pockit.com"));
    }

    @Test
    @DisplayName("POST /api/auth/recuperar-password debe retornar 400 cuando el correo es invalido")
    void shouldReturn400WhenRecoverPasswordEmailInvalid() throws Exception {
        RecuperarPasswordRequestDto invalidDto = new RecuperarPasswordRequestDto("correo-invalido");

        mockMvc.perform(post("/api/auth/recuperar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_VALIDATION_ERROR));
    }

    @Test
    @DisplayName("POST /api/auth/restablecer-password debe retornar 200 OK cuando el token y password son validos")
    void shouldResetPasswordAndReturn200() throws Exception {
        RestablecerPasswordRequestDto dto = new RestablecerPasswordRequestDto("valid-token", "NuevaClave@2026");

        mockMvc.perform(post("/api/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(BaseConstants.MSG_RESTABLECER_PASSWORD_OK));

        org.mockito.Mockito.verify(restablecerPasswordUseCase)
                .ejecutar(new RestablecerPasswordCommand("valid-token", "NuevaClave@2026"));
    }

    @Test
    @DisplayName("POST /api/auth/restablecer-password debe retornar 400 cuando el token es invalido o expirado")
    void shouldReturn400WhenResetTokenInvalid() throws Exception {
        org.mockito.Mockito.doThrow(new BaseDomainException(
                400, BaseConstants.CODE_RESET_TOKEN_INVALIDO, BaseConstants.MSG_RESET_TOKEN_INVALIDO
        )).when(restablecerPasswordUseCase).ejecutar(any(RestablecerPasswordCommand.class));

        RestablecerPasswordRequestDto dto = new RestablecerPasswordRequestDto("bad-token", "NuevaClave@2026");

        mockMvc.perform(post("/api/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_RESET_TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("POST /api/auth/restablecer-password debe retornar 422 cuando la nueva contrasena es debil")
    void shouldReturn422WhenResetPasswordIsWeak() throws Exception {
        org.mockito.Mockito.doThrow(new BaseDomainException(
                422, BaseConstants.CODE_PASSWORD_DEBIL, "Contrasena debil"
        )).when(restablecerPasswordUseCase).ejecutar(any(RestablecerPasswordCommand.class));

        RestablecerPasswordRequestDto dto = new RestablecerPasswordRequestDto("valid-token", "debil");

        mockMvc.perform(post("/api/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_PASSWORD_DEBIL));
    }
}
