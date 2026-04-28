package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sesiones.sesiones_backend.config.LLMProperties;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeepSeekClient implements LLMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final int LOG_PREVIEW_LIMIT = 240;

    private final RestClient deepSeekRestClient;
    private final LLMProperties llmProperties;

    @Override
    public String generate(String prompt) {
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            throw new BusinessRuleException("DEEPSEEK_API_KEY no esta configurada en el entorno");
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
            llmProperties.getModel(),
            List.of(new ChatMessage("user", prompt)),
            llmProperties.getTemperature()
        );

        LOGGER.info(
            "Enviando solicitud a DeepSeek. modelo={}, timeoutSegundos={}, temperature={}, promptLength={}",
            llmProperties.getModel(),
            llmProperties.getTimeoutSeconds(),
            llmProperties.getTemperature(),
            prompt.length()
        );
        LOGGER.debug("Prompt enviado a DeepSeek: {}", truncate(prompt));

        try {
            DeepSeekChatResponse response = deepSeekRestClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + llmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DeepSeekChatResponse.class);

            String generatedText = extractText(response);
            LOGGER.info(
                "Respuesta recibida desde DeepSeek. responseId={}, choices={}, outputLength={}",
                response == null ? null : response.id(),
                response == null || response.choices() == null ? 0 : response.choices().size(),
                generatedText.length()
            );
            LOGGER.debug("Respuesta textual de DeepSeek: {}", truncate(generatedText));
            return generatedText;
        } catch (RestClientResponseException exception) {
            LOGGER.error(
                "DeepSeek respondio con error HTTP. status={}, body={}",
                exception.getStatusCode(),
                truncate(exception.getResponseBodyAsString())
            );
            throw buildExternalServiceException(exception);
        } catch (ResourceAccessException exception) {
            LOGGER.error("No fue posible conectar con DeepSeek", exception);
            throw new ExternalServiceException("No fue posible conectar con DeepSeek", exception);
        }
    }

    private ExternalServiceException buildExternalServiceException(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String responseBody = truncate(exception.getResponseBodyAsString());
        List<String> details = new ArrayList<String>();

        if (responseBody != null && !responseBody.isBlank()) {
            details.add("Detalle DeepSeek: " + responseBody);
        }

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            details.add("Revisa la cuota, el plan y el billing del proyecto en DeepSeek.");
            return new ExternalServiceException(
                HttpStatus.TOO_MANY_REQUESTS,
                "DeepSeek no puede generar la sesion porque la cuenta o proyecto no tiene cuota disponible en este momento.",
                details,
                exception
            );
        }

        if (status == HttpStatus.PAYMENT_REQUIRED) {
            details.add("La cuenta de DeepSeek no tiene saldo suficiente para procesar la solicitud.");
            details.add("Recarga saldo o revisa el billing del proyecto en DeepSeek.");
            return new ExternalServiceException(
                HttpStatus.PAYMENT_REQUIRED,
                "DeepSeek no puede generar la sesion porque la cuenta no tiene saldo disponible.",
                details,
                exception
            );
        }

        if (status == HttpStatus.UNAUTHORIZED) {
            details.add("Verifica que DEEPSEEK_API_KEY sea valida y pertenezca al proyecto correcto.");
            return new ExternalServiceException(
                HttpStatus.BAD_GATEWAY,
                "DeepSeek rechazo la autenticacion al generar la sesion.",
                details,
                exception
            );
        }

        return new ExternalServiceException(
            HttpStatus.BAD_GATEWAY,
            "DeepSeek devolvio un error HTTP al generar la sesion.",
            details,
            exception
        );
    }

    private String extractText(DeepSeekChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ExternalServiceException("DeepSeek devolvio una respuesta vacia");
        }

        return response.choices().stream()
            .map(Choice::message)
            .filter(message -> message != null && message.content() != null && !message.content().isBlank())
            .map(ChatMessage::content)
            .findFirst()
            .map(String::trim)
            .orElseThrow(() -> new ExternalServiceException("DeepSeek no devolvio contenido utilizable"));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= LOG_PREVIEW_LIMIT) {
            return value;
        }
        return value.substring(0, LOG_PREVIEW_LIMIT) + "...";
    }

    private record DeepSeekChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekChatResponse(String id, List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatMessage(String role, String content) {
    }
}
