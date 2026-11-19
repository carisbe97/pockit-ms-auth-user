package com.pockit.finance.auth.common.infrastructure.utilities;

import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageUtilTest {

    @Test
    @DisplayName("Debe validar y leer correctamente una imagen PNG y JPEG valida")
    void shouldValidateValidPngAndJpeg() throws Exception {
        byte[] pngBytes = createImageBytes("png");
        MockMultipartFile pngFile = new MockMultipartFile("avatar", "avatar.png", "image/png", pngBytes);

        ImageUtil.ValidatedImage pngResult = ImageUtil.validateAndRead(pngFile);
        assertThat(pngResult.bytes()).isEqualTo(pngBytes);
        assertThat(pngResult.contentType()).isEqualTo("image/png");
        assertThat(pngResult.extension()).isEqualTo("png");

        byte[] jpgBytes = createImageBytes("jpg");
        MockMultipartFile jpgFile = new MockMultipartFile("avatar", "avatar.jpg", "IMAGE/JPEG", jpgBytes);

        ImageUtil.ValidatedImage jpgResult = ImageUtil.validateAndRead(jpgFile);
        assertThat(jpgResult.bytes()).isEqualTo(jpgBytes);
        assertThat(jpgResult.contentType()).isEqualTo("image/jpeg");
        assertThat(jpgResult.extension()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("Debe validar magic bytes RIFF/WEBP para image/webp y rechazar WEBP corrupto")
    void shouldValidateWebpMagicBytes() {
        byte[] validWebp = new byte[]{
                'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
        MockMultipartFile webpFile = new MockMultipartFile("avatar", "avatar.webp", "image/webp", validWebp);

        ImageUtil.ValidatedImage result = ImageUtil.validateAndRead(webpFile);
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.extension()).isEqualTo("webp");

        MockMultipartFile badWebp = new MockMultipartFile(
                "avatar", "bad.webp", "image/webp", "NOT_A_WEBP_FILE".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> ImageUtil.validateAndRead(badWebp))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_CORRUPTA));

        MockMultipartFile shortWebp = new MockMultipartFile(
                "avatar", "short.webp", "image/webp", new byte[]{'R', 'I', 'F', 'F'}
        );
        assertThatThrownBy(() -> ImageUtil.validateAndRead(shortWebp))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_CORRUPTA));
    }

    @Test
    @DisplayName("Debe rechazar archivo nulo, vacio, >2MB, MIME invalido, corrupto o error de IO")
    void shouldRejectInvalidImages() throws Exception {
        assertThatThrownBy(() -> ImageUtil.validateAndRead(null))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_VACIA));

        MockMultipartFile emptyFile = new MockMultipartFile("avatar", "empty.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> ImageUtil.validateAndRead(emptyFile))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_VACIA));

        byte[] oversized = new byte[BaseConstants.MAX_IMAGE_SIZE_BYTES + 1];
        MockMultipartFile bigFile = new MockMultipartFile("avatar", "big.png", "image/png", oversized);
        assertThatThrownBy(() -> ImageUtil.validateAndRead(bigFile))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> {
                    ImagenException ie = (ImagenException) ex;
                    assertThat(ie.getStatusCode()).isEqualTo(413);
                    assertThat(ie.getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO);
                });

        MockMultipartFile invalidMime = new MockMultipartFile(
                "avatar", "doc.gif", "image/gif", createImageBytes("png")
        );
        assertThatThrownBy(() -> ImageUtil.validateAndRead(invalidMime))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_FORMATO_INVALIDO));

        MockMultipartFile corruptPng = new MockMultipartFile(
                "avatar", "corrupt.png", "image/png", "not-an-image".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> ImageUtil.validateAndRead(corruptPng))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_CORRUPTA));

        MultipartFile ioFailFile = mock(MultipartFile.class);
        when(ioFailFile.isEmpty()).thenReturn(false);
        when(ioFailFile.getBytes()).thenThrow(new IOException("Error de lectura"));
        assertThatThrownBy(() -> ImageUtil.validateAndRead(ioFailFile))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> assertThat(((ImagenException) ex).getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_CORRUPTA));
    }

    private byte[] createImageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
