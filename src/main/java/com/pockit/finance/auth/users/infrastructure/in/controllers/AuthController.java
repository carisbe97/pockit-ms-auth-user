package com.pockit.finance.auth.users.infrastructure.in.controllers;

import com.pockit.finance.auth.common.infrastructure.utilities.ImageUtil;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.ICreateUsuarioUseCase;
import com.pockit.finance.auth.users.domain.ports.in.ILoginUseCase;
import com.pockit.finance.auth.users.domain.ports.in.ILogoutUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IRefreshTokenUseCase;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.AuthTokensResponseDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.CreateUsuarioDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.LoginRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.LogoutRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.RefreshTokenRequestDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.UsuarioResponseDto;
import com.pockit.finance.auth.users.infrastructure.config.in.mappers.IUsuarioWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Endpoints publicos de registro y autenticacion de usuarios")
public class AuthController {

    private final ICreateUsuarioUseCase createUsuarioUseCase;
    private final ILoginUseCase loginUseCase;
    private final IRefreshTokenUseCase refreshTokenUseCase;
    private final ILogoutUseCase logoutUseCase;
    private final IUsuarioWebMapper usuarioWebMapper;

    @Operation(summary = "Registrar usuario (JSON)", description = "Registra un nuevo usuario sin avatar usando JSON.")
    @PostMapping(value = {"/registro", "/register"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponseDto> registrarJson(@Valid @RequestBody CreateUsuarioDto dto) {
        CreateUsuarioCommand command = new CreateUsuarioCommand(
                dto.usrCorreo(),
                dto.usrPassword(),
                dto.usrNombre(),
                dto.usrApellido(),
                dto.usrEdad(),
                null,
                null,
                null
        );
        Usuario creado = createUsuarioUseCase.ejecutar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioWebMapper.toResponseDto(creado));
    }

    @Operation(summary = "Registrar usuario (Multipart)", description = "Registra un nuevo usuario con avatar opcional.")
    @PostMapping(value = {"/registro", "/register"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioResponseDto> registrarMultipart(
            @RequestPart("datos") @Valid CreateUsuarioDto dto,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        byte[] avatarBytes = null;
        String avatarContentType = null;
        String avatarExtension = null;

        if (avatar != null) {
            ImageUtil.ValidatedImage validatedImage = ImageUtil.validateAndRead(avatar);
            avatarBytes = validatedImage.bytes();
            avatarContentType = validatedImage.contentType();
            avatarExtension = validatedImage.extension();
        }

        CreateUsuarioCommand command = new CreateUsuarioCommand(
                dto.usrCorreo(),
                dto.usrPassword(),
                dto.usrNombre(),
                dto.usrApellido(),
                dto.usrEdad(),
                avatarBytes,
                avatarContentType,
                avatarExtension
        );
        Usuario creado = createUsuarioUseCase.ejecutar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioWebMapper.toResponseDto(creado));
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica al usuario y retorna access token JWT y refresh token opaco.")
    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        AuthResult result = loginUseCase.ejecutar(new LoginCommand(
                dto.usrCorreo(),
                dto.usrPassword(),
                dto.dispositivo()
        ));
        return ResponseEntity.ok(usuarioWebMapper.toAuthTokensResponseDto(result));
    }

    @Operation(summary = "Refrescar sesion", description = "Rota el refresh token de un solo uso y emite un nuevo par de tokens.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto dto) {
        AuthResult result = refreshTokenUseCase.ejecutar(new RefreshTokenCommand(
                dto.refreshToken(),
                dto.dispositivo()
        ));
        return ResponseEntity.ok(usuarioWebMapper.toAuthTokensResponseDto(result));
    }

    @Operation(summary = "Cerrar sesion", description = "Revoca el refresh token indicado.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDto dto) {
        logoutUseCase.ejecutar(new LogoutCommand(dto.refreshToken()));
        return ResponseEntity.noContent().build();
    }
}
