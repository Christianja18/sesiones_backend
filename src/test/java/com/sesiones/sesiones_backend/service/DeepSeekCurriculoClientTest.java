package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

class DeepSeekCurriculoClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseJsonInsideMarkdownFence() throws Exception {
        DeepSeekCurriculoClient client = buildClient();

        String content = """
            ```json
            {"items":[{"area":"Matematica","competencia":"Resuelve problemas","capacidades":[],"desempenos":[],"nivel":"Primaria","grado":"1ro","ciclo":"III","confianza":0.93}]}
            ```
            """;

        String json = client.extractJsonPayload(content);

        assertEquals("Matematica", objectMapper.readTree(json).path("items").get(0).path("area").asText());
    }

    @Test
    void shouldParseJsonEvenWithLeadingText() throws Exception {
        DeepSeekCurriculoClient client = buildClient();

        String json = client.extractJsonPayload("""
            Aqui tienes el JSON solicitado:
            {"items":[{"area":"Comunicacion","competencia":"Lee diversos tipos de textos","capacidades":[],"desempenos":[],"nivel":"Primaria","grado":"2do","ciclo":"III","confianza":0.88}]}
            """);

        assertEquals("Comunicacion", objectMapper.readTree(json).path("items").get(0).path("area").asText());
    }

    @Test
    void shouldFailWhenResponseDoesNotContainJson() {
        DeepSeekCurriculoClient client = buildClient();

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> client.extractJsonPayload("No encontre informacion util en este fragmento.")
        );

        assertEquals("DeepSeek no devolvio JSON curricular interpretable", exception.getMessage());
    }

    @Test
    void shouldExtractContentFromArrayResponse() {
        DeepSeekCurriculoClient client = buildClient();
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode choices = response.putArray("choices");
        ObjectNode choice = choices.addObject();
        ArrayNode content = choice.putObject("message").putArray("content");
        content.addObject().put("text", "Intro");
        content.addObject().put("text", "{\"items\":[]}");

        String extracted = client.extractContent(response);

        assertEquals("Intro\n{\"items\":[]}", extracted);
    }

    @Test
    void shouldMarkGatewayTimeoutAsRetryableStatus() {
        DeepSeekCurriculoClient client = buildClient();

        assertEquals(true, client.isRetryableStatus(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT));
        assertEquals(true, client.isRetryableStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        assertEquals(false, client.isRetryableStatus(org.springframework.http.HttpStatus.BAD_REQUEST));
    }

    private DeepSeekCurriculoClient buildClient() {
        return new DeepSeekCurriculoClient(
            RestClient.builder(),
            objectMapper,
            "https://api.deepseek.com",
            "test-key",
            "deepseek-chat",
            60,
            2,
            10
        );
    }
}
