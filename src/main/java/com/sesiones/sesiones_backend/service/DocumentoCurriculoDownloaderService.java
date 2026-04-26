package com.sesiones.sesiones_backend.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.sesiones.sesiones_backend.exception.ExternalServiceException;

@Service
public class DocumentoCurriculoDownloaderService {

    private final RestClient restClient;

    public DocumentoCurriculoDownloaderService(
        RestClient.Builder restClientBuilder,
        @Value("${app.curriculo.documento.timeout-seconds:60}") long timeoutSeconds
    ) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        this.restClient = restClientBuilder
            .requestFactory(requestFactory)
            .build();
    }

    public byte[] download(String archivoUrl) {
        try {
            byte[] contenido = restClient.get()
                .uri(archivoUrl)
                .retrieve()
                .body(byte[].class);

            if (contenido == null || contenido.length == 0) {
                throw new ExternalServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible descargar el documento curricular",
                    List.of("La URL no devolvio contenido util"),
                    null
                );
            }

            return contenido;
        } catch (RestClientResponseException exception) {
            throw new ExternalServiceException(
                HttpStatus.BAD_GATEWAY,
                "No fue posible descargar el documento curricular",
                List.of("Respuesta HTTP " + exception.getStatusCode().value()),
                exception
            );
        } catch (ExternalServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible descargar el documento curricular", exception);
        }
    }
}
