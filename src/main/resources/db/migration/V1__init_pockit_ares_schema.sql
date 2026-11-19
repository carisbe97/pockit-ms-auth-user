-- ============================================================================
-- Flyway Migration V1: Inicializacion del esquema Pockit Finance (Version Camilo)
-- Ambientes: pockit-ares (DEV) | pockit-hermes (QA OCI) | pockit-zeus (PROD OCI)
-- Estrategia Hibrida de PK:
--   * UUID: usuarios, transacciones
--   * BIGSERIAL / SERIAL: sesiones_refresh, cuentas, categorias, etiquetas, series
-- Nota: Sin BEGIN/COMMIT ni DROP CASCADE (Flyway administra la transaccion).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Tipos Enumerados de Dominio
CREATE TYPE public.naturaleza_cuenta AS ENUM (
    'BANCO',
    'BILLETERA'
);

CREATE TYPE public.naturaleza_movimiento AS ENUM (
    'INGRESO',
    'GASTO'
);

CREATE TYPE public.estado_transaccion AS ENUM (
    'PENDIENTE',
    'REALIZADA'
);

CREATE TYPE public.tipo_registro_transaccion AS ENUM (
    'ESTANDAR',
    'TRANSFERENCIA',
    'RECURRENTE',
    'PROGRAMADA',
    'CUOTA'
);

CREATE TYPE public.tipo_serie AS ENUM (
    'RECURRENTE',
    'CUOTA'
);

CREATE TYPE public.frecuencia_serie AS ENUM (
    'DIARIA',
    'SEMANAL',
    'QUINCENAL',
    'MENSUAL',
    'ANUAL'
);

-- 2. Usuarios [PK: UUID] (Spring Security BCrypt + Supabase Storage Avatar)
CREATE TABLE public.usuarios (
    usr_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usr_correo VARCHAR(255) NOT NULL UNIQUE,
    usr_password_hash VARCHAR(255) NOT NULL,
    usr_nombre VARCHAR(100) NOT NULL,
    usr_apellido VARCHAR(100) NOT NULL,
    usr_edad SMALLINT NOT NULL CHECK (usr_edad >= 0 AND usr_edad <= 130),
    usr_avatar_url TEXT NULL,
    usr_activo BOOLEAN NOT NULL DEFAULT TRUE,
    usr_reset_token_hash VARCHAR(255) NULL,
    usr_reset_expira_en TIMESTAMPTZ NULL,
    usr_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usr_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Sesiones de Refresh Token [PK: BIGSERIAL]
CREATE TABLE public.sesiones_refresh (
    srf_id BIGSERIAL PRIMARY KEY,
    srf_usr_id UUID NOT NULL REFERENCES public.usuarios(usr_id) ON DELETE CASCADE,
    srf_token_hash VARCHAR(255) NOT NULL UNIQUE,
    srf_expira_en TIMESTAMPTZ NOT NULL,
    srf_revocado BOOLEAN NOT NULL DEFAULT FALSE,
    srf_dispositivo VARCHAR(120) NULL,
    srf_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_srf_usr_id ON public.sesiones_refresh(srf_usr_id);

-- 4. Cuentas Principales y Subcuentas / Bolsillos de Ahorro [PK: BIGSERIAL]
CREATE TABLE public.cuentas (
    cta_id BIGSERIAL PRIMARY KEY,
    cta_usr_id UUID NOT NULL REFERENCES public.usuarios(usr_id) ON DELETE CASCADE,
    cta_cta_padre_id BIGINT NULL REFERENCES public.cuentas(cta_id) ON DELETE CASCADE,
    cta_naturaleza public.naturaleza_cuenta NULL,
    cta_nombre VARCHAR(100) NOT NULL,
    cta_saldo_inicial NUMERIC(14, 2) NOT NULL DEFAULT 0,
    cta_saldo_actual NUMERIC(14, 2) NOT NULL DEFAULT 0,
    cta_color VARCHAR(20) NOT NULL,
    cta_icono VARCHAR(50) NOT NULL,
    cta_activa BOOLEAN NOT NULL DEFAULT TRUE,
    cta_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cta_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cta_no_autopadre CHECK (cta_cta_padre_id IS NULL OR cta_cta_padre_id <> cta_id),
    CONSTRAINT chk_cta_naturaleza_principal CHECK (
        (cta_cta_padre_id IS NULL AND cta_naturaleza IS NOT NULL)
        OR (cta_cta_padre_id IS NOT NULL)
    )
);

CREATE INDEX idx_cta_usr_id ON public.cuentas(cta_usr_id);
CREATE INDEX idx_cta_cta_padre_id ON public.cuentas(cta_cta_padre_id);

-- 5. Categorias [PK: SERIAL]
CREATE TABLE public.categorias (
    cat_id SERIAL PRIMARY KEY,
    cat_usr_id UUID NOT NULL REFERENCES public.usuarios(usr_id) ON DELETE CASCADE,
    cat_nombre VARCHAR(100) NOT NULL,
    cat_naturaleza public.naturaleza_movimiento NOT NULL,
    cat_color VARCHAR(20) NOT NULL,
    cat_icono VARCHAR(50) NOT NULL,
    cat_es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    cat_activa BOOLEAN NOT NULL DEFAULT TRUE,
    cat_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cat_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_cat_usr_nombre_naturaleza
    ON public.categorias(cat_usr_id, LOWER(cat_nombre), cat_naturaleza);

-- 6. Etiquetas [PK: BIGSERIAL]
CREATE TABLE public.etiquetas (
    etq_id BIGSERIAL PRIMARY KEY,
    etq_usr_id UUID NOT NULL REFERENCES public.usuarios(usr_id) ON DELETE CASCADE,
    etq_nombre VARCHAR(80) NOT NULL,
    etq_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    etq_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_etq_usr_nombre
    ON public.etiquetas(etq_usr_id, LOWER(etq_nombre));

-- 7. Series [PK: BIGSERIAL]
CREATE TABLE public.series (
    ser_id BIGSERIAL PRIMARY KEY,
    ser_tipo public.tipo_serie NOT NULL,
    ser_frecuencia public.frecuencia_serie NOT NULL DEFAULT 'MENSUAL',
    ser_intervalo INTEGER NOT NULL DEFAULT 1 CHECK (ser_intervalo >= 1),
    ser_total_cuotas INTEGER NULL CHECK (ser_total_cuotas IS NULL OR ser_total_cuotas >= 1),
    ser_monto_total NUMERIC(14, 2) NULL CHECK (ser_monto_total IS NULL OR ser_monto_total > 0),
    ser_fecha_inicio DATE NOT NULL,
    ser_fecha_fin DATE NULL,
    ser_proxima_fecha DATE NULL,
    ser_activa BOOLEAN NOT NULL DEFAULT TRUE,
    ser_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ser_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ser_cuotas CHECK (
        (ser_tipo = 'CUOTA' AND ser_total_cuotas IS NOT NULL AND ser_monto_total IS NOT NULL)
        OR (ser_tipo = 'RECURRENTE')
    )
);

CREATE INDEX idx_ser_activas_proxima
    ON public.series(ser_activa, ser_proxima_fecha)
    WHERE ser_activa = TRUE;

-- 8. Transacciones [PK: UUID] (Idempotencia Offline-First + Comprobante Storage)
CREATE TABLE public.transacciones (
    trx_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trx_cta_id BIGINT NOT NULL REFERENCES public.cuentas(cta_id) ON DELETE RESTRICT,
    trx_cat_id INTEGER NOT NULL REFERENCES public.categorias(cat_id) ON DELETE RESTRICT,
    trx_ser_id BIGINT NULL REFERENCES public.series(ser_id) ON DELETE SET NULL,
    trx_trx_par_id UUID NULL UNIQUE REFERENCES public.transacciones(trx_id) ON DELETE CASCADE,
    trx_monto NUMERIC(14, 2) NOT NULL CHECK (trx_monto > 0),
    trx_fecha DATE NOT NULL,
    trx_descripcion TEXT NULL,
    trx_comprobante_url TEXT NULL,
    trx_estado public.estado_transaccion NOT NULL DEFAULT 'REALIZADA',
    trx_tipo_registro public.tipo_registro_transaccion NOT NULL DEFAULT 'ESTANDAR',
    trx_numero_cuota INTEGER NULL CHECK (trx_numero_cuota IS NULL OR trx_numero_cuota >= 1),
    trx_es_excepcion_serie BOOLEAN NOT NULL DEFAULT FALSE,
    trx_recordatorio_en TIMESTAMPTZ NULL,
    trx_sincronizado_en TIMESTAMPTZ NULL,
    trx_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trx_actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_trx_no_autopar CHECK (trx_trx_par_id IS NULL OR trx_trx_par_id <> trx_id),
    CONSTRAINT chk_trx_origen_coherente CHECK (
        NOT (trx_trx_par_id IS NOT NULL AND trx_ser_id IS NOT NULL)
    )
);

CREATE INDEX idx_trx_cta_fecha ON public.transacciones(trx_cta_id, trx_fecha DESC);
CREATE INDEX idx_trx_cat_fecha ON public.transacciones(trx_cat_id, trx_fecha DESC);
CREATE INDEX idx_trx_estado ON public.transacciones(trx_estado);
CREATE INDEX idx_trx_ser_id ON public.transacciones(trx_ser_id) WHERE trx_ser_id IS NOT NULL;

-- 9. Tabla Puente Transacciones - Etiquetas [PK: (UUID, BIGINT)]
CREATE TABLE public.transaccion_etiquetas (
    txe_trx_id UUID NOT NULL REFERENCES public.transacciones(trx_id) ON DELETE CASCADE,
    txe_etq_id BIGINT NOT NULL REFERENCES public.etiquetas(etq_id) ON DELETE CASCADE,
    txe_creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (txe_trx_id, txe_etq_id)
);

CREATE INDEX idx_txe_etq_id ON public.transaccion_etiquetas(txe_etq_id);

-- 10. Control de Versionamiento del Esquema
CREATE TABLE IF NOT EXISTS public.esquema_versiones (
    ver_id SERIAL PRIMARY KEY,
    ver_base_datos VARCHAR(50) NOT NULL DEFAULT 'pockit-ares',
    ver_version VARCHAR(20) NOT NULL,
    ver_descripcion TEXT NOT NULL,
    ver_ejecutado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO public.esquema_versiones (ver_base_datos, ver_version, ver_descripcion)
VALUES (
    'pockit-ares',
    'v2.1.0-camilo',
    'Migracion inicial Flyway V1: estrategia hibrida UUID + BIGSERIAL/SERIAL, Spring Security propio y URLs Supabase Storage.'
);
