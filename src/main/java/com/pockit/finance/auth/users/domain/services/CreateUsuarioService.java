package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.EmailUtil;
import com.pockit.finance.auth.common.domain.utilities.PasswordUtil;
import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.ICreateUsuarioUseCase;
import com.pockit.finance.auth.users.domain.ports.out.ICategoriaSistemaPort;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.util.Objects;

/**
 * Servicio de dominio puro que implementa el caso de uso de registro de usuario.
 */
public class CreateUsuarioService implements ICreateUsuarioUseCase {

    private static final String CODE_CORREO_YA_REGISTRADO = "CORREO_YA_REGISTRADO";
    private static final String CODE_CORREO_INVALIDO = "CORREO_INVALIDO";
    private static final String CODE_PASSWORD_DEBIL = "PASSWORD_DEBIL";
    private static final String CODE_EDAD_INVALIDA = "EDAD_INVALIDA";
    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final int MIN_EDAD = 0;
    private static final int MAX_EDAD = 130;

    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final IPasswordHasherPort passwordHasherPort;
    private final ICategoriaSistemaPort categoriaSistemaPort;
    private final IStoragePort storagePort;

    public CreateUsuarioService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ICategoriaSistemaPort categoriaSistemaPort,
            IStoragePort storagePort
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
        this.categoriaSistemaPort = Objects.requireNonNull(categoriaSistemaPort);
        this.storagePort = Objects.requireNonNull(storagePort);
    }

    @Override
    public Usuario ejecutar(CreateUsuarioCommand command) {
        if (command == null) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "La solicitud de registro es obligatoria.");
        }

        String correoNormalizado = EmailUtil.normalize(command.correo());
        if (!EmailUtil.isValid(correoNormalizado)) {
            throw new BaseDomainException(422, CODE_CORREO_INVALIDO, "El correo electronico no tiene un formato valido.");
        }

        if (!PasswordUtil.isStrong(command.password())) {
            throw new BaseDomainException(
                    422,
                    CODE_PASSWORD_DEBIL,
                    "La contrasena debe tener al menos 8 caracteres, mayuscula, minuscula, numero y caracter especial."
            );
        }

        if (command.edad() == null || command.edad() < MIN_EDAD || command.edad() > MAX_EDAD) {
            throw new BaseDomainException(422, CODE_EDAD_INVALIDA, "La edad debe estar comprendida entre 0 y 130 anos.");
        }

        if (command.nombre() == null || command.nombre().isBlank()
                || command.apellido() == null || command.apellido().isBlank()) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "El nombre y el apellido son obligatorios.");
        }

        if (usuarioRepositoryPort.existsByCorreo(correoNormalizado)) {
            throw new BaseDomainException(409, CODE_CORREO_YA_REGISTRADO, "El correo electronico ya se encuentra registrado.");
        }

        String passwordHash = passwordHasherPort.hash(command.password());

        Usuario nuevoUsuario = Usuario.builder()
                .usrCorreo(correoNormalizado)
                .usrPasswordHash(passwordHash)
                .usrNombre(command.nombre().trim())
                .usrApellido(command.apellido().trim())
                .usrEdad(command.edad())
                .usrActivo(Boolean.TRUE)
                .build();

        Usuario guardado = usuarioRepositoryPort.save(nuevoUsuario);
        categoriaSistemaPort.crearCategoriasSistema(guardado.getUsrId());

        if (command.avatarBytes() != null && command.avatarBytes().length > 0) {
            String avatarUrl = storagePort.subirAvatar(
                    guardado.getUsrId(),
                    command.avatarBytes(),
                    command.avatarContentType(),
                    command.avatarExtension()
            );
            guardado.actualizarAvatar(avatarUrl);
            guardado = usuarioRepositoryPort.save(guardado);
        }

        return guardado;
    }
}
