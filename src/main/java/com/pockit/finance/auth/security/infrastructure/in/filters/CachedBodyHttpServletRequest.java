package com.pockit.finance.auth.security.infrastructure.in.filters;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Envuelve el request HTTP reemplazando su cuerpo por el JSON desencriptado en UTF-8.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, String plainJsonBody) {
        super(request);
        this.cachedBody = plainJsonBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // Lectura sincrona sobre arreglo en memoria
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return cachedBody.length;
    }

    @Override
    public long getContentLengthLong() {
        return cachedBody.length;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
    }

    @Override
    public String getHeader(String name) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            return String.valueOf(cachedBody.length);
        }
        if ("Content-Type".equalsIgnoreCase(name)) {
            return getContentType();
        }
        return super.getHeader(name);
    }
}
