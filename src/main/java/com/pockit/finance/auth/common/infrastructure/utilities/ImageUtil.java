package com.pockit.finance.auth.common.infrastructure.utilities;

import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Utilidad de infraestructura para validar y leer imagenes (<= 2MB, JPEG/PNG/WEBP, integridad).
 */
public final class ImageUtil {

    private static final int WEBP_MIN_HEADER_BYTES = 12;

    private ImageUtil() {
    }

    public record ValidatedImage(byte[] bytes, String contentType, String extension) {
    }

    public static ValidatedImage validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImagenException(BaseConstants.CODE_IMAGEN_VACIA, "La imagen no puede estar vacia.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "No fue posible leer la imagen.", ex);
        }

        if (bytes.length == 0) {
            throw new ImagenException(BaseConstants.CODE_IMAGEN_VACIA, "La imagen no puede estar vacia.");
        }

        if (bytes.length > BaseConstants.MAX_IMAGE_SIZE_BYTES) {
            throw new ImagenException(413, BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO, BaseConstants.MSG_MAX_UPLOAD_EXCEEDED);
        }

        String rawContentType = file.getContentType();
        String contentType = rawContentType == null ? "" : rawContentType.trim().toLowerCase(Locale.ROOT);
        if (!BaseConstants.ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new ImagenException(
                    BaseConstants.CODE_IMAGEN_FORMATO_INVALIDO,
                    "Formato de imagen no permitido. Solo se aceptan JPEG, PNG o WEBP."
            );
        }

        if ("image/webp".equals(contentType)) {
            if (!hasValidWebpMagicBytes(bytes)) {
                throw new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "La imagen WEBP esta corrupta o no es valida.");
            }
            return new ValidatedImage(bytes, contentType, "webp");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "La imagen esta corrupta o no es legible.");
            }
        } catch (IOException ex) {
            throw new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "La imagen esta corrupta o no es legible.", ex);
        }

        String extension = "image/jpeg".equals(contentType) ? "jpg" : "png";
        return new ValidatedImage(bytes, contentType, extension);
    }

    private static boolean hasValidWebpMagicBytes(byte[] bytes) {
        if (bytes.length < WEBP_MIN_HEADER_BYTES) {
            return false;
        }
        // RIFF....WEBP
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
