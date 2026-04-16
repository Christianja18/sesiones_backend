package com.sesiones.sesiones_backend.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@Service
public class DeepSeekCurriculoClient implements CurriculoLlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public DeepSeekCurriculoClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${app.llm.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${app.llm.deepseek.api-key:}") String apiKey,
        @Value("${app.llm.deepseek.model:deepseek-chat}") String model,
        @Value("${app.llm.deepseek.timeout-seconds:60}") long timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;

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
    public CurriculoDocumentoParseResponse extraerCurriculo(String textoPdf, String areaSugerida, String gradoSugerido, String nivelSugerido) {
        if (apiKey.isBlank()) {
            throw new BusinessRuleException("DEEPSEEK_API_KEY no esta configurada para procesar el PDF curricular");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.1);
        payload.put("messages", buildMessages(textoPdf, areaSugerida, gradoSugerido, nivelSugerido));

        try {
            JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

            String content = extractContent(response);
            return objectMapper.readValue(stripMarkdownCodeFence(content), CurriculoDocumentoParseResponse.class);
        } catch (RestClientResponseException exception) {
            throw new BusinessRuleException("DeepSeek no pudo estructurar el contenido curricular: " + exception.getResponseBodyAsString());
        } catch (Exception exception) {
            throw new BusinessRuleException("No fue posible interpretar la respuesta de DeepSeek para el curriculo");
        }
    }

    private Object buildMessages(String textoPdf, String areaSugerida, String gradoSugerido, String nivelSugerido) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Extrae y estructura el curriculo del siguiente texto de un PDF del Curriculo Nacional del Peru. ");
        userPrompt.append("Responde solo JSON valido con esta estructura exacta: ");
        userPrompt.append("{\"niveles\":[{\"nombre\":\"Primaria\",\"grados\":[{\"nombre\":\"1ro\",\"areas\":[{\"nombre\":\"Matematica\",\"competencias\":[{\"descripcion\":\"...\",\"capacidades\":[\"...\"],\"desempenos\":[\"...\"]}]}]}]}]}. ");
        userPrompt.append("No inventes informacion. Omite elementos que no existan en el texto. ");
        if (nivelSugerido != null) {
            userPrompt.append("Prioriza el nivel sugerido: ").append(nivelSugerido).append(". ");
        }
        if (gradoSugerido != null) {
            userPrompt.append("Prioriza el grado sugerido: ").append(gradoSugerido).append(". ");
        }
        if (areaSugerida != null) {
            userPrompt.append("Prioriza el area sugerida: ").append(areaSugerida).append(". ");
        }
        userPrompt.append("Texto del PDF:\n").append(textoPdf);

        return java.util.List.of(
            Map.of(
                "role", "system",
                "content", "Eres un asistente experto en curriculo peruano. Extraes datos curriculares y respondes solo JSON valido."
            ),
            Map.of(
                "role", "user",
                "content", userPrompt.toString()
            )
        );
    }

    private String extractContent(JsonNode response) {
        JsonNode choices = response == null ? null : response.path("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new BusinessRuleException("DeepSeek no devolvio contenido util para el curriculo");
        }

        String content = choices.get(0).path("message").path("content").asText(null);
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessRuleException("DeepSeek devolvio una respuesta vacia para el curriculo");
        }
        return content;
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
}
