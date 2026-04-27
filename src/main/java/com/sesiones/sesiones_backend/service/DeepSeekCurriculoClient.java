package com.sesiones.sesiones_backend.service;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.exception.ExternalServiceException;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;

@Service
public class DeepSeekCurriculoClient implements CurriculoLlmClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekCurriculoClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final long timeoutSeconds;
    private final int maxRetries;
    private final long retryDelayMs;

    public DeepSeekCurriculoClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${app.llm.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${app.llm.deepseek.api-key:}") String apiKey,
        @Value("${app.llm.deepseek.model:deepseek-chat}") String model,
        @Value("${app.llm.deepseek.timeout-seconds:60}") long timeoutSeconds,
        @Value("${app.llm.deepseek.max-retries:2}") int maxRetries,
        @Value("${app.llm.deepseek.retry-delay-ms:2000}") long retryDelayMs
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryDelayMs = Math.max(0L, retryDelayMs);

        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public CurriculoDocumentoParseResponse extraerCurriculo(String contenidoChunk, DocumentoCurriculoTipo tipoDocumento) {
        if (apiKey.isBlank()) {
            throw new BusinessRuleException("DEEPSEEK_API_KEY no esta configurada para procesar el documento curricular");
        }
        if (tipoDocumento == null) {
            throw new BusinessRuleException("El tipo del documento curricular es obligatorio para procesar la ingesta");
        }
        if (contenidoChunk == null || contenidoChunk.isBlank()) {
            throw new BusinessRuleException("El contenido del chunk curricular es obligatorio");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.1);
        payload.put("messages", buildMessages(contenidoChunk, tipoDocumento));
        payload.put("response_format", Map.of("type", "json_object"));

        JsonNode response = null;
        String content = null;
        String jsonPayload = null;
        int totalAttempts = maxRetries + 1;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            response = null;
            content = null;
            jsonPayload = null;

            try {
                LOGGER.info(
                    "Solicitando interpretacion curricular a DeepSeek. model={}, chunkLength={}, attempt={}/{}",
                    model,
                    contenidoChunk.length(),
                    attempt,
                    totalAttempts
                );

                response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

                content = extractContent(response);
                jsonPayload = extractJsonPayload(content);
                LOGGER.info(
                    "Respuesta curricular de DeepSeek interpretada correctamente. attempt={}/{}, contentPreview={}, jsonPreview={}",
                    attempt,
                    totalAttempts,
                    abbreviate(content),
                    abbreviate(jsonPayload)
                );
                return objectMapper.readValue(jsonPayload, CurriculoDocumentoParseResponse.class);
            } catch (RestClientResponseException exception) {
                if (shouldRetry(exception.getStatusCode(), attempt, totalAttempts)) {
                    logRetryableHttpFailure(exception, attempt, totalAttempts);
                    waitBeforeRetry(attempt);
                    continue;
                }

                LOGGER.error(
                    "DeepSeek devolvio un error HTTP al estructurar el contenido curricular. status={}, body={}, attempt={}/{}",
                    exception.getStatusCode(),
                    abbreviate(exception.getResponseBodyAsString()),
                    attempt,
                    totalAttempts,
                    exception
                );
                throw buildHttpServiceException(exception, attempt, totalAttempts);
            } catch (RestClientException exception) {
                if (isTimeoutException(exception) && attempt < totalAttempts) {
                    LOGGER.warn(
                        "DeepSeek excedio el tiempo de espera al estructurar el contenido curricular. timeoutSeconds={}, attempt={}/{}. Reintentando.",
                        requestTimeoutSeconds(),
                        attempt,
                        totalAttempts,
                        exception
                    );
                    waitBeforeRetry(attempt);
                    continue;
                }

                if (isTimeoutException(exception)) {
                    LOGGER.error(
                        "DeepSeek excedio el tiempo de espera al estructurar el contenido curricular. timeoutSeconds={}, attempt={}/{}",
                        requestTimeoutSeconds(),
                        attempt,
                        totalAttempts,
                        exception
                    );
                    throw new ExternalServiceException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "DeepSeek excedio el tiempo de espera al procesar un fragmento curricular tras " + totalAttempts
                            + " intentos. Incrementa DEEPSEEK_TIMEOUT_SECONDS o reduce CURRICULO_CHUNK_MAX_CHARS.",
                        exception
                    );
                }

                LOGGER.error("Fallo la comunicacion con DeepSeek al estructurar el contenido curricular", exception);
                throw new ExternalServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible comunicarse correctamente con DeepSeek para procesar el curriculo",
                    exception
                );
            } catch (BusinessRuleException exception) {
                LOGGER.error(
                    "No fue posible interpretar la respuesta curricular de DeepSeek. contentPreview={}, jsonPreview={}, responsePreview={}, attempt={}/{}",
                    abbreviate(content),
                    abbreviate(jsonPayload),
                    abbreviate(response == null ? null : response.toString()),
                    attempt,
                    totalAttempts,
                    exception
                );
                throw exception;
            } catch (Exception exception) {
                LOGGER.error(
                    "Fallo inesperado al interpretar la respuesta curricular de DeepSeek. contentPreview={}, jsonPreview={}, responsePreview={}, attempt={}/{}",
                    abbreviate(content),
                    abbreviate(jsonPayload),
                    abbreviate(response == null ? null : response.toString()),
                    attempt,
                    totalAttempts,
                    exception
                );
                throw new BusinessRuleException("No fue posible interpretar la respuesta de DeepSeek para el curriculo");
            }
        }

        throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "No fue posible completar la solicitud a DeepSeek", null);
    }

    private Object buildMessages(String contenidoChunk, DocumentoCurriculoTipo tipoDocumento) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Actua como especialista en documentos curriculares oficiales del MINEDU Peru. ");
        userPrompt.append("Estas procesando un fragmento de un documento curricular oficial. ");
        userPrompt.append("Tipo de documento registrado: ")
            .append(tipoDocumento.getDatabaseValue())
            .append(". ");
        userPrompt.append("Extrae informacion curricular estructurada sin inventar datos. ");
        userPrompt.append("Reglas importantes: ");
        userPrompt.append("1. Si el tipo es 'curriculo', extrae area, competencia, capacidades, estandares por ciclo, nivel y ciclo; desempenos debe ser []. ");
        userPrompt.append("2. Si el tipo es 'programa', extrae area, competencia, capacidades, estandares por ciclo, desempenos por grado, nivel, grado y ciclo. ");
        userPrompt.append("3. No mezclar areas, competencias, ciclos o grados diferentes en un mismo item. ");
        userPrompt.append("4. Cada item debe representar una unidad curricular coherente. ");
        userPrompt.append("5. No duplicar capacidades, estandares ni desempenos dentro del mismo item. ");
        userPrompt.append("6. Mantener el texto oficial literal o lo mas cercano posible; no resumir ni reinterpretar. ");
        userPrompt.append("7. Si un campo no existe en el fragmento, usar cadena vacia o lista vacia. ");
        userPrompt.append("8. No inventar grado; si aparece como Primer grado, Segundo grado, etc., normalizalo a 1ro, 2do, 3ero, 4to, 5to o 6to. ");
        userPrompt.append("9. Normaliza ciclo a III, IV, V, VI o VII cuando aparezca. ");
        userPrompt.append("10. confianza debe estar entre 0.0 y 1.0 segun claridad del fragmento. ");
        userPrompt.append("Devuelve unicamente JSON valido con esta estructura exacta: ");
        userPrompt.append("{\"items\":[{\"area\":\"\",\"competencia\":\"\",\"capacidades\":[\"\"],\"estandares\":[\"\"],\"desempenos\":[\"\"],");
        userPrompt.append("\"nivel\":\"\",\"grado\":\"\",\"ciclo\":\"\",\"confianza\":0.0}]}. ");
        appendExtractionRules(userPrompt, tipoDocumento);
        userPrompt.append("Si no hay informacion curricular util, responde {\"items\":[]}. ");
        userPrompt.append("Contenido del chunk:\n").append(contenidoChunk);

        return List.of(
            Map.of(
                "role",
                "system",
                "content",
                "Eres experto en curriculo peruano. Extraes datos curriculares y respondes solo JSON valido."
            ),
            Map.of(
                "role",
                "user",
                "content",
                userPrompt.toString()
            )
        );
    }

    private void appendExtractionRules(StringBuilder userPrompt, DocumentoCurriculoTipo tipoDocumento) {
        if (tipoDocumento == DocumentoCurriculoTipo.CURRICULO) {
            userPrompt.append("Para Curriculo Nacional: competencias, capacidades y estandares son la informacion principal. ");
            userPrompt.append("No extraigas desempenos desde Curriculo Nacional; desempenos debe ser []. ");
            userPrompt.append("No confundas estandares por ciclo con desempenos por grado. ");
            return;
        }

        userPrompt.append("Para Programa Curricular: el documento puede traer area, competencia, capacidades, estandares y desempenos. ");
        userPrompt.append("Extrae desempenos oficiales por grado solo cuando el grado este visible en el fragmento. ");
        userPrompt.append("Incluye capacidades y estandares de la misma competencia cuando aparezcan en el fragmento para poder relacionar el desempeno. ");
    }

    String extractContent(JsonNode response) {
        JsonNode choices = response == null ? null : response.path("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new BusinessRuleException("DeepSeek no devolvio contenido util para el curriculo");
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        String content = extractTextContent(contentNode);
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessRuleException("DeepSeek devolvio una respuesta vacia para el curriculo");
        }
        return content;
    }

    String extractJsonPayload(String content) {
        String normalized = stripMarkdownCodeFence(content);
        if (normalized.startsWith("{") || normalized.startsWith("[")) {
            return normalized;
        }

        int objectStart = normalized.indexOf('{');
        int arrayStart = normalized.indexOf('[');
        int start = resolveJsonStart(objectStart, arrayStart);
        if (start < 0) {
            throw new BusinessRuleException("DeepSeek no devolvio JSON curricular interpretable");
        }

        String extracted = extractBalancedJson(normalized, start);
        if (extracted == null) {
            throw new BusinessRuleException("DeepSeek devolvio una estructura JSON incompleta para el curriculo");
        }
        return extracted;
    }

    private String extractTextContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (!contentNode.isArray()) {
            return contentNode.toString();
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : contentNode) {
            if (item == null || item.isNull()) {
                continue;
            }

            String text = item.path("text").asText(null);
            if (text == null || text.isBlank()) {
                text = item.asText(null);
            }
            if (text == null || text.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(text);
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private int resolveJsonStart(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }

    private String extractBalancedJson(String value, int start) {
        List<Character> stack = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == '{' || current == '[') {
                stack.add(current);
                continue;
            }
            if ((current == '}' || current == ']') && !stack.isEmpty()) {
                char expected = current == '}' ? '{' : '[';
                char last = stack.get(stack.size() - 1);
                if (last != expected) {
                    return null;
                }

                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    return value.substring(start, index + 1).trim();
                }
            }
        }

        return null;
    }

    private String stripMarkdownCodeFence(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```json")) {
            normalized = normalized.substring(7).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }

        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }
        return normalized;
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 400) {
            return normalized;
        }
        return normalized.substring(0, 400) + "...";
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long requestTimeoutSeconds() {
        return timeoutSeconds;
    }

    boolean isRetryableStatus(HttpStatusCode status) {
        return status != null && (
            status.value() == HttpStatus.GATEWAY_TIMEOUT.value()
                || status.value() == HttpStatus.BAD_GATEWAY.value()
                || status.value() == HttpStatus.SERVICE_UNAVAILABLE.value()
                || status.value() == HttpStatus.TOO_MANY_REQUESTS.value()
        );
    }

    private boolean shouldRetry(HttpStatusCode status, int attempt, int totalAttempts) {
        return isRetryableStatus(status) && attempt < totalAttempts;
    }

    private void logRetryableHttpFailure(RestClientResponseException exception, int attempt, int totalAttempts) {
        LOGGER.warn(
            "DeepSeek devolvio un error HTTP transitorio. status={}, attempt={}/{}. Reintentando. bodyPreview={}",
            exception.getStatusCode(),
            attempt,
            totalAttempts,
            abbreviate(exception.getResponseBodyAsString()),
            exception
        );
    }

    private ExternalServiceException buildHttpServiceException(
        RestClientResponseException exception,
        int attempt,
        int totalAttempts
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus resolvedStatus = status == null ? HttpStatus.BAD_GATEWAY : status;

        if (resolvedStatus == HttpStatus.GATEWAY_TIMEOUT) {
            return new ExternalServiceException(
                HttpStatus.GATEWAY_TIMEOUT,
                "DeepSeek devolvio 504 Gateway Timeout al procesar un fragmento curricular tras " + attempt
                    + " de " + totalAttempts + " intentos. Reintenta o reduce CURRICULO_CHUNK_MAX_CHARS.",
                exception
            );
        }

        if (resolvedStatus == HttpStatus.TOO_MANY_REQUESTS) {
            return new ExternalServiceException(
                HttpStatus.TOO_MANY_REQUESTS,
                "DeepSeek rechazo temporalmente la solicitud por limite de uso. Reintenta en unos minutos.",
                exception
            );
        }

        return new ExternalServiceException(
            HttpStatus.BAD_GATEWAY,
            "DeepSeek no pudo estructurar el contenido curricular. status=" + resolvedStatus.value(),
            exception
        );
    }

    private void waitBeforeRetry(int attempt) {
        if (retryDelayMs <= 0) {
            return;
        }

        long delay = retryDelayMs * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException(
                HttpStatus.BAD_GATEWAY,
                "La espera para reintentar la solicitud a DeepSeek fue interrumpida",
                exception
            );
        }
    }
}
