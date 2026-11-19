package com.pockit.finance.auth.users.infrastructure.out.persistence.adapters;

import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoriaSistemaPersistenceAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CategoriaSistemaPersistenceAdapter adapter;

    @Test
    @DisplayName("Debe insertar las dos categorias de sistema (GASTO e INGRESO) usando CAST a naturaleza_movimiento")
    void shouldInsertSystemCategories() {
        UUID usrId = UUID.randomUUID();

        adapter.crearCategoriasSistema(usrId);

        verify(jdbcTemplate).update(
                contains("CAST(? AS public.naturaleza_movimiento)"),
                eq(usrId),
                eq(BaseConstants.CAT_SISTEMA_TRANSFERENCIA_SALIENTE),
                eq("GASTO"),
                eq(BaseConstants.CAT_SISTEMA_COLOR_DEFAULT),
                eq(BaseConstants.CAT_SISTEMA_ICONO_SALIENTE)
        );

        verify(jdbcTemplate).update(
                contains("CAST(? AS public.naturaleza_movimiento)"),
                eq(usrId),
                eq(BaseConstants.CAT_SISTEMA_TRANSFERENCIA_ENTRANTE),
                eq("INGRESO"),
                eq(BaseConstants.CAT_SISTEMA_COLOR_DEFAULT),
                eq(BaseConstants.CAT_SISTEMA_ICONO_ENTRANTE)
        );
    }
}
