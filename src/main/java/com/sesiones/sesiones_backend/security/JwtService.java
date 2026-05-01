package com.sesiones.sesiones_backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.config.SecurityProperties;
import com.sesiones.sesiones_backend.entity.Docente;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(ObjectMapper objectMapper, Clock clock, SecurityProperties securityProperties) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        String jwtSecret = securityProperties.getJwt().getSecret() == null
            ? null
            : securityProperties.getJwt().getSecret().trim();
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = securityProperties.getJwt().getExpirationMinutes() * 60;
    }

    public String createToken(Docente docente) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", docente.getEmail());
        payload.put("userId", docente.getId());
        payload.put("role", docente.getRol().getNombre());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public JwtClaims validate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtTokenException("Token JWT mal formado");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtTokenException("Firma JWT invalida");
        }

        Map<String, Object> header = decodeJson(parts[0]);
        if (!"HS256".equals(header.get("alg"))) {
            throw new JwtTokenException("Algoritmo JWT no soportado");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        String email = readString(payload, "sub");
        String role = readString(payload, "role");
        Integer userId = readInteger(payload, "userId");
        Instant issuedAt = Instant.ofEpochSecond(readLong(payload, "iat"));
        Instant expiresAt = Instant.ofEpochSecond(readLong(payload, "exp"));
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw new JwtTokenException("Token JWT expirado");
        }
        return new JwtClaims(userId, email, role, issuedAt, expiresAt);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(values));
        } catch (JsonProcessingException exception) {
            throw new JwtTokenException("No se pudo generar el token JWT");
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(encoded), MAP_TYPE);
        } catch (IllegalArgumentException | IOException exception) {
            throw new JwtTokenException("Token JWT invalido");
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new JwtTokenException("No se pudo firmar el token JWT");
        }
    }

    private String readString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new JwtTokenException("Claim JWT requerido ausente: " + key);
    }

    private Integer readInteger(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new JwtTokenException("Claim JWT requerido ausente: " + key);
    }

    private long readLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new JwtTokenException("Claim JWT requerido ausente: " + key);
    }
}
