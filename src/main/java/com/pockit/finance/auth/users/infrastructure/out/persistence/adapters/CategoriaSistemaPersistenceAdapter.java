package com.pockit.finance.auth.users.infrastructure.out.persistence.adapters;

import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.users.domain.ports.out.ICategoriaSistemaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoriaSistemaPersistenceAdapter implements ICategoriaSistemaPort {

    private static final String INSERT_CATEGORIA_SISTEMA_SQL = """
            INSERT INTO public.categorias
                (cat_usr_id, cat_nombre, cat_naturaleza, cat_color, cat_icono, cat_es_sistema, cat_activa)
            VALUES (?, ?, CAST(? AS public.naturaleza_movimiento), ?, ?, TRUE, TRUE)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void crearCategoriasSistema(UUID usrId) {
        jdbcTemplate.update(
                INSERT_CATEGORIA_SISTEMA_SQL,
                usrId,
                BaseConstants.CAT_SISTEMA_TRANSFERENCIA_SALIENTE,
                "GASTO",
                BaseConstants.CAT_SISTEMA_COLOR_DEFAULT,
                BaseConstants.CAT_SISTEMA_ICONO_SALIENTE
        );

        jdbcTemplate.update(
                INSERT_CATEGORIA_SISTEMA_SQL,
                usrId,
                BaseConstants.CAT_SISTEMA_TRANSFERENCIA_ENTRANTE,
                "INGRESO",
                BaseConstants.CAT_SISTEMA_COLOR_DEFAULT,
                BaseConstants.CAT_SISTEMA_ICONO_ENTRANTE
        );
    }
}
