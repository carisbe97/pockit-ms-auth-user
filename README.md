# Pockit Finance — `pockit-ms-auth-user`

Microservicio de **Autenticación (Spring Security + BCrypt + JWT propio)**, **Gestión de Usuarios**, **Migraciones Master Flyway** y **Gestión de Avatares en Supabase Storage** construido en **Java 21 + Spring Boot 3.3.5 + Maven** bajo **Arquitectura Hexagonal (Puertos y Adaptadores)**.

## Ambientes Mitológicos
* ⚔️ **`ares`** (`DEV` Local) → Base de datos `pockit-ares`
* 🪽 **`hermes`** (`QA` / Test Server OCI) → Base de datos `pockit-hermes`
* ⚡ **`zeus`** (`PROD` OCI) → Base de datos `pockit-zeus`

## Ejecución y Verificación Local
1. Copia `.env.example` a `.env` y configura tus variables de Supabase (`pockit-ares`).
2. Ejecuta pruebas unitarias y verificación de cobertura **JaCoCo (>= 80%)**:
   ```bash
   mvn clean verify
   ```
3. Levanta el microservicio:
   ```bash
   mvn spring-boot:run
   ```
4. Endpoints base:
   * Healthcheck: `GET http://localhost:8081/actuator/health`
   * Swagger UI: `http://localhost:8081/swagger-ui.html`
