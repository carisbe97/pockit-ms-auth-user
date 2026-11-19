package com.pockit.finance.auth.users.infrastructure.out.storage;

import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageAdapterTest {

    private static final String SUPABASE_URL = "https://xyz.supabase.co";
    private static final String SERVICE_KEY = "service-role-secret";
    private static final String BUCKET = "avatars";

    @Test
    @DisplayName("Debe subir avatar a Supabase Storage y retornar URL publica")
    void shouldUploadAvatarAndReturnPublicUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageAdapter adapter = new SupabaseStorageAdapter(
                builder.build(), SUPABASE_URL + "/", SERVICE_KEY, BUCKET
        );

        UUID usrId = UUID.randomUUID();
        byte[] bytes = new byte[]{10, 20, 30};

        server.expect(requestTo(SUPABASE_URL + "/storage/v1/object/avatars/" + usrId + "/avatar.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SERVICE_KEY))
                .andExpect(header("apikey", SERVICE_KEY))
                .andExpect(header("x-upsert", "true"))
                .andExpect(header("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andRespond(withSuccess());

        String url = adapter.subirAvatar(usrId, bytes, "image/png", "png");

        server.verify();
        assertThat(url).isEqualTo(SUPABASE_URL + "/storage/v1/object/public/avatars/" + usrId + "/avatar.png");
    }

    @Test
    @DisplayName("Debe lanzar ImagenException 502 CODE_IMAGEN_SUBIDA_ERROR cuando Supabase responde error")
    void shouldThrow502WhenSupabaseFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageAdapter adapter = new SupabaseStorageAdapter(
                builder.build(), SUPABASE_URL, SERVICE_KEY, BUCKET
        );

        UUID usrId = UUID.randomUUID();
        byte[] bytes = new byte[]{10, 20, 30};

        server.expect(requestTo(SUPABASE_URL + "/storage/v1/object/avatars/" + usrId + "/avatar.jpg"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.subirAvatar(usrId, bytes, "image/jpeg", "jpg"))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> {
                    ImagenException ie = (ImagenException) ex;
                    assertThat(ie.getStatusCode()).isEqualTo(502);
                    assertThat(ie.getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_SUBIDA_ERROR);
                });

        server.verify();
    }

    @Test
    @DisplayName("Debe lanzar ImagenException 502 si Supabase no esta configurado")
    void shouldThrow502WhenNotConfigured() {
        SupabaseStorageAdapter adapter = new SupabaseStorageAdapter(
                RestClient.builder().build(), "", "", "avatars"
        );

        assertThatThrownBy(() -> adapter.subirAvatar(UUID.randomUUID(), new byte[]{1}, "image/png", "png"))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> {
                    ImagenException ie = (ImagenException) ex;
                    assertThat(ie.getStatusCode()).isEqualTo(502);
                    assertThat(ie.getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_SUBIDA_ERROR);
                });
    }
}
