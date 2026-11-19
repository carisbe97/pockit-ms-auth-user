package com.pockit.finance.auth.users.infrastructure.out.storage;

import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Slf4j
@Component
public class SupabaseStorageAdapter implements IStoragePort {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String avatarBucket;

    public SupabaseStorageAdapter(
            RestClient supabaseRestClient,
            @Value("${app.supabase.url:}") String supabaseUrl,
            @Value("${app.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.supabase.storage.avatar-bucket:avatars}") String avatarBucket
    ) {
        this.restClient = supabaseRestClient;
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.replaceAll("/+$", "");
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey;
        this.avatarBucket = avatarBucket == null || avatarBucket.isBlank() ? "avatars" : avatarBucket;
    }

    @Override
    public String subirAvatar(UUID usrId, byte[] bytes, String contentType, String extension) {
        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
            throw new ImagenException(
                    502,
                    BaseConstants.CODE_IMAGEN_SUBIDA_ERROR,
                    "El servicio de almacenamiento de imagenes no esta configurado."
            );
        }

        String objectPath = String.format("%s/avatar.%s", usrId, extension);
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, avatarBucket, objectPath);

        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();

            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, avatarBucket, objectPath);
        } catch (RestClientException ex) {
            log.error("Error subiendo avatar a Supabase Storage para usuario {}: {}", usrId, ex.getMessage());
            throw new ImagenException(
                    502,
                    BaseConstants.CODE_IMAGEN_SUBIDA_ERROR,
                    "No fue posible subir el avatar al servicio de almacenamiento.",
                    ex
            );
        }
    }
}
