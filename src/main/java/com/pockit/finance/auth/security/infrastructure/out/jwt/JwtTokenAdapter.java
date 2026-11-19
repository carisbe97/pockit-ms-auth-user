package com.pockit.finance.auth.security.infrastructure.out.jwt;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.models.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements ITokenPort {

    private static final int MIN_HS512_SECRET_BYTES = 64;

    private final SecretKey secretKey;
    private final String issuer;
    private final long expirationSeconds;
    private final Clock clock;

    @Autowired
    public JwtTokenAdapter(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer:pockit-ms-auth-user}") String issuer,
            @Value("${app.security.jwt.expiration-seconds:900}") long expirationSeconds
    ) {
        this(secret, issuer, expirationSeconds, Clock.systemUTC());
    }

    JwtTokenAdapter(String secret, String issuer, long expirationSeconds, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_HS512_SECRET_BYTES) {
            throw new IllegalStateException("app.security.jwt.secret debe tener al menos 64 bytes para HS512.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    @Override
    public String generarAccessToken(Usuario usuario) {
        if (usuario == null || usuario.getUsrId() == null) {
            throw new BaseDomainException(400, BaseConstants.CODE_TOKEN_INVALIDO, "Usuario invalido para emitir token.");
        }
        Instant now = Instant.now(clock);
        Instant exp = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(usuario.getUsrId().toString())
                .claim("correo", usuario.getUsrCorreo())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public UUID validarYExtraerUsuarioId(String token) {
        if (token == null || token.isBlank()) {
            throw new BaseDomainException(401, BaseConstants.CODE_TOKEN_INVALIDO, BaseConstants.MSG_TOKEN_INVALIDO);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException ex) {
            throw new BaseDomainException(401, BaseConstants.CODE_TOKEN_EXPIRADO, BaseConstants.MSG_TOKEN_EXPIRADO);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BaseDomainException(401, BaseConstants.CODE_TOKEN_INVALIDO, BaseConstants.MSG_TOKEN_INVALIDO);
        }
    }

    @Override
    public long obtenerExpiracionSegundos() {
        return expirationSeconds;
    }
}
