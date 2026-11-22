package com.pockit.finance.auth.users.infrastructure.in.controllers;

import com.pockit.finance.auth.common.infrastructure.utilities.ImageUtil;
import com.pockit.finance.auth.security.infrastructure.in.annotations.CurrentUserId;
import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IGetUsuarioPerfilUseCase;
import com.pockit.finance.auth.users.domain.ports.in.IUpdateAvatarUseCase;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.UsuarioResponseDto;
import com.pockit.finance.auth.users.infrastructure.config.in.mappers.IUsuarioWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints protegidos para gestion de perfil de usuario")
public class UsuarioController {

    private final IGetUsuarioPerfilUseCase getUsuarioPerfilUseCase;
    private final IUpdateAvatarUseCase updateAvatarUseCase;
    private final IUsuarioWebMapper usuarioWebMapper;

    @Operation(summary = "Obtener perfil autenticado", description = "Retorna el perfil del usuario autenticado mediante JWT.")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDto> obtenerPerfil(@CurrentUserId UUID usrId) {
        Usuario usuario = getUsuarioPerfilUseCase.ejecutar(usrId);
        return ResponseEntity.ok(usuarioWebMapper.toResponseDto(usuario));
    }

    @Operation(summary = "Actualizar avatar", description = "Actualiza la imagen de avatar del usuario autenticado.")
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioResponseDto> actualizarAvatar(
            @CurrentUserId UUID usrId,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        ImageUtil.ValidatedImage validatedImage = ImageUtil.validateAndRead(avatar);
        Usuario actualizado = updateAvatarUseCase.ejecutar(new UpdateAvatarCommand(
                usrId,
                validatedImage.bytes(),
                validatedImage.contentType(),
                validatedImage.extension()
        ));
        return ResponseEntity.ok(usuarioWebMapper.toResponseDto(actualizado));
    }
}
