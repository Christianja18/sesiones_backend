package com.sesiones.sesiones_backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Login login = new Login();
    private Cors cors = new Cors();
    private Cookie cookie = new Cookie();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 120;
    }

    @Data
    public static class Login {
        private int maxAttempts = 5;
        private long windowSeconds = 900;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));
    }

    @Data
    public static class Cookie {
        private String name = "SESSION_TOKEN";
        private String path = "/";
        private String domain;
        private boolean secure = false;
        private String sameSite = "Lax";
    }
}
