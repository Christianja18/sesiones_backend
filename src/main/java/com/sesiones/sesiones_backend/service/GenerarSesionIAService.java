package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.dto.TextoReferenciaResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.EstandarAprendizaje;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenerarSesionIAService {

    private static final int MAX_LLM_ATTEMPTS = 2;

    private final LLMClient llmClient;
    private final ReferenceResolver referenceResolver;
    private final CapacidadRepository capacidadRepository;
    private final DesempenoRepository desempenoRepository;
    private final EstandarAprendizajeRepository estandarAprendizajeRepository;
    private final TemplateSessionGeneratorService templateSessionGeneratorService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SesionResponse execute(GenerateSesionRequest request) {
        NivelEducativo nivel = referenceResolver.findNivel(request.getNivelId());
        Grado grado = referenceResolver.findGradoByNivel(request.getGradoId(), request.getNivelId());
        Area area = referenceResolver.findArea(request.getAreaId());
        Competencia competencia = referenceResolver.findCompetenciaByArea(request.getCompetenciaId(), request.getAreaId());
        List<Capacidad> capacidades = capacidadRepository.findByCompetenciaIdOrderByIdAsc(competencia.getId());
        List<EstandarAprendizaje> estandares = grado.getCiclo() == null
            ? List.of()
            : estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(
                competencia.getId(),
                grado.getCiclo().getId()
            );
        List<Desempeno> desempenos = desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(grado.getId(), competencia.getId());

        GeneratedSesionContent generatedContent = generateWithRetry(
            request,
            nivel,
            grado,
            area,
            competencia,
            capacidades,
            estandares,
            desempenos
        );
        if (generatedContent == null) {
            return templateSessionGeneratorService.generate(request, grado, area, competencia, capacidades, estandares, desempenos);
        }

        return SesionResponse.builder()
            .titulo(generatedContent.getTitulo())
            .proposito(generatedContent.getProposito())
            .duracionMinutos(request.getDuracionMinutos())
            .generadoPorIa(true)
            .competencias(toReferenceResponses(competencia))
            .capacidades(toCapacidadResponses(capacidades))
            .estandares(toEstandarResponses(estandares))
            .desempenos(toDesempenoResponses(desempenos))
            .actividades(generatedContent.getActividades())
            .criteriosEvaluacion(generatedContent.getCriterios())
            .evidencias(generatedContent.getEvidencias())
            .instrumentoEvaluacion(generatedContent.getInstrumentoEvaluacion())
            .build();
    }

    private GeneratedSesionContent generateWithRetry(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos
    ) {
        BusinessRuleException lastException = null;

        for (int attempt = 1; attempt <= MAX_LLM_ATTEMPTS; attempt++) {
            boolean strictRetry = attempt > 1;
            try {
                StructureContent structure = generateStructure(
                    request,
                    nivel,
                    grado,
                    area,
                    competencia,
                    capacidades,
                    estandares,
                    desempenos,
                    strictRetry
                );
                ActividadesSesionDto activities = generateActivities(
                    request,
                    nivel,
                    grado,
                    area,
                    competencia,
                    structure,
                    capacidades,
                    estandares,
                    desempenos,
                    strictRetry
                );
                EvaluationContent evaluation = generateEvaluation(
                    request,
                    nivel,
                    grado,
                    area,
                    competencia,
                    structure,
                    capacidades,
                    estandares,
                    desempenos,
                    strictRetry
                );

                validateBasicCoherence(activities, evaluation.getCriterios(), evaluation.getEvidencias());

                return new GeneratedSesionContent(
                    structure.getTitulo(),
                    structure.getProposito(),
                    activities,
                    evaluation.getCriterios(),
                    evaluation.getEvidencias(),
                    evaluation.getInstrumentoEvaluacion()
                );
            } catch (BusinessRuleException exception) {
                lastException = exception;
            }
        }

        if (lastException != null) {
            return null;
        }
        throw new BusinessRuleException("No fue posible generar una sesion valida con la IA");
    }

    private StructureContent generateStructure(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = buildStructurePrompt(request, nivel, grado, area, competencia, capacidades, estandares, desempenos, strictRetry);
        JsonNode root = parseResponse(llmClient.generate(prompt));

        String titulo = readRequiredText(root, "titulo");
        String proposito = readRequiredText(root, "proposito");

        readRequiredTextArray(root, "competencias");
        readRequiredTextArray(root, "capacidades");
        readRequiredTextArray(root, "desempenos");

        return new StructureContent(titulo, proposito);
    }

    private ActividadesSesionDto generateActivities(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = buildActivitiesPrompt(request, nivel, grado, area, competencia, structure, capacidades, estandares, desempenos, strictRetry);
        JsonNode root = parseResponse(llmClient.generate(prompt));

        return ActividadesSesionDto.builder()
            .inicio(readRequiredTextArray(root, "inicio"))
            .desarrollo(readRequiredTextArray(root, "desarrollo"))
            .cierre(readRequiredTextArray(root, "cierre"))
            .build();
    }

    private EvaluationContent generateEvaluation(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = buildEvaluationPrompt(request, nivel, grado, area, competencia, structure, capacidades, estandares, desempenos, strictRetry);
        JsonNode root = parseResponse(llmClient.generate(prompt));

        List<String> criterios = readRequiredTextArray(root, "criterios");
        List<String> evidencias = readRequiredTextArray(root, "evidencias");

        JsonNode instrumentNode = root.path("instrumento");
        if (!instrumentNode.isObject()) {
            throw new BusinessRuleException("La respuesta de IA no contiene un instrumento valido");
        }

        String tipo = readRequiredText(instrumentNode, "tipo");
        try {
            InstrumentoTipo.fromDatabaseValue(tipo);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("La respuesta de IA contiene un tipo de instrumento invalido");
        }

        InstrumentoEvaluacionDto instrumento = InstrumentoEvaluacionDto.builder()
            .tipo(tipo.trim())
            .detalle(readRequiredTextArray(instrumentNode, "detalle"))
            .build();

        return new EvaluationContent(criterios, evidencias, instrumento);
    }

    private String buildStructurePrompt(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = """
            Genera una sesion de aprendizaje basada en el Curriculo Nacional del Peru.

            Datos:
            - Nivel: %s
            - Grado: %s
            - Area: %s
            - Competencia: %s
            - Tema: %s
            - Contexto: %s
            - Duracion: %s minutos
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Responde SOLO en JSON valido:
            {
              "titulo": "",
              "proposito": "",
              "competencias": [],
              "capacidades": [],
              "desempenos": []
            }
            """.formatted(
            nivel.getNombre(),
            grado.getNombre(),
            area.getNombre(),
            competencia.getDescripcion(),
            request.getTema().trim(),
            request.getContexto().trim(),
            request.getDuracionMinutos(),
            joinCapacidades(capacidades),
            joinEstandares(estandares),
            joinDesempenos(desempenos)
        );

        if (!strictRetry) {
            return prompt;
        }

        return prompt + """

            IMPORTANTE:
            - No uses markdown ni texto adicional
            - Todas las listas deben existir y tener al menos un elemento
            - Usa "no_disponible" si alguna referencia no puede redactarse
            """;
    }

    private String buildActivitiesPrompt(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = """
            Genera la secuencia didactica de una sesion de aprendizaje.

            Incluye:
            - Inicio (motivacion + saberes previos)
            - Desarrollo (actividades activas)
            - Cierre (metacognicion)

            Datos:
            - Nivel: %s
            - Grado: %s
            - Area: %s
            - Competencia: %s
            - Tema: %s
            - Contexto: %s
            - Titulo: %s
            - Proposito: %s
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Devuelve SOLO en JSON valido:
            {
              "inicio": [],
              "desarrollo": [],
              "cierre": []
            }
            """.formatted(
            nivel.getNombre(),
            grado.getNombre(),
            area.getNombre(),
            competencia.getDescripcion(),
            request.getTema().trim(),
            request.getContexto().trim(),
            structure.getTitulo(),
            structure.getProposito(),
            joinCapacidades(capacidades),
            joinEstandares(estandares),
            joinDesempenos(desempenos)
        );

        if (!strictRetry) {
            return prompt;
        }

        return prompt + """

            IMPORTANTE:
            - Cada lista debe contener al menos un elemento
            - No uses markdown ni texto fuera del JSON
            - Mantente alineado al contexto peruano y al nivel educativo
            """;
    }

    private String buildEvaluationPrompt(
        GenerateSesionRequest request,
        NivelEducativo nivel,
        Grado grado,
        Area area,
        Competencia competencia,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        boolean strictRetry
    ) {
        String prompt = """
            Genera evaluacion formativa alineada a competencias.

            Datos:
            - Nivel: %s
            - Grado: %s
            - Area: %s
            - Competencia: %s
            - Tema: %s
            - Contexto: %s
            - Titulo: %s
            - Proposito: %s
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Devuelve SOLO en JSON valido:
            {
              "criterios": [],
              "evidencias": [],
              "instrumento": {
                "tipo": "rubrica",
                "detalle": []
              }
            }
            """.formatted(
            nivel.getNombre(),
            grado.getNombre(),
            area.getNombre(),
            competencia.getDescripcion(),
            request.getTema().trim(),
            request.getContexto().trim(),
            structure.getTitulo(),
            structure.getProposito(),
            joinCapacidades(capacidades),
            joinEstandares(estandares),
            joinDesempenos(desempenos)
        );

        if (!strictRetry) {
            return prompt;
        }

        return prompt + """

            IMPORTANTE:
            - El instrumento.tipo solo puede ser "rubrica" o "lista_cotejo"
            - Cada lista debe contener al menos un elemento
            - No uses markdown ni texto fuera del JSON
            """;
    }

    private String joinCapacidades(List<Capacidad> capacidades) {
        if (capacidades == null || capacidades.isEmpty()) {
            return "- no_disponible";
        }
        return capacidades.stream()
            .map(Capacidad::getDescripcion)
            .collect(Collectors.joining(System.lineSeparator() + "- ", "- ", ""));
    }

    private String joinEstandares(List<EstandarAprendizaje> estandares) {
        if (estandares == null || estandares.isEmpty()) {
            return "- no_disponible";
        }
        return estandares.stream()
            .map(EstandarAprendizaje::getDescripcion)
            .collect(Collectors.joining(System.lineSeparator() + "- ", "- ", ""));
    }

    private String joinDesempenos(List<Desempeno> desempenos) {
        if (desempenos == null || desempenos.isEmpty()) {
            return "- no_disponible";
        }
        return desempenos.stream()
            .map(Desempeno::getDescripcion)
            .collect(Collectors.joining(System.lineSeparator() + "- ", "- ", ""));
    }

    private JsonNode parseResponse(String rawResponse) {
        try {
            return objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("La respuesta de IA no contiene JSON valido");
        }
    }

    private String readRequiredText(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isTextual() || node.asText().trim().isEmpty()) {
            throw new BusinessRuleException("La respuesta de IA no contiene el campo obligatorio " + fieldName);
        }
        return node.asText().trim();
    }

    private List<String> readRequiredTextArray(JsonNode root, String fieldName) {
        JsonNode arrayNode = root.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new BusinessRuleException("La respuesta de IA no contiene la lista obligatoria " + fieldName);
        }

        List<String> values = new ArrayList<String>();
        for (JsonNode item : arrayNode) {
            if (item.isTextual() && !item.asText().trim().isEmpty()) {
                values.add(item.asText().trim());
            }
        }

        if (values.isEmpty()) {
            throw new BusinessRuleException("La respuesta de IA contiene una lista vacia en " + fieldName);
        }
        return values;
    }

    private void validateBasicCoherence(ActividadesSesionDto actividades, List<String> criterios, List<String> evidencias) {
        int totalActivities = actividades.getInicio().size()
            + actividades.getDesarrollo().size()
            + actividades.getCierre().size();

        if (totalActivities < 3) {
            throw new BusinessRuleException("La respuesta de IA no tiene suficientes actividades para una sesion valida");
        }
        if (criterios.isEmpty() || evidencias.isEmpty()) {
            throw new BusinessRuleException("La respuesta de IA debe incluir criterios y evidencias");
        }
    }

    private List<TextoReferenciaResponse> toReferenceResponses(Competencia competencia) {
        return List.of(new TextoReferenciaResponse(competencia.getId(), competencia.getDescripcion()));
    }

    private List<TextoReferenciaResponse> toCapacidadResponses(List<Capacidad> capacidades) {
        return capacidades.stream()
            .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
            .collect(Collectors.toList());
    }

    private List<TextoReferenciaResponse> toEstandarResponses(List<EstandarAprendizaje> estandares) {
        return estandares.stream()
            .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
            .collect(Collectors.toList());
    }

    private List<TextoReferenciaResponse> toDesempenoResponses(List<Desempeno> desempenos) {
        return desempenos.stream()
            .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
            .collect(Collectors.toList());
    }

    @lombok.Value
    private static class StructureContent {
        String titulo;
        String proposito;
    }

    @lombok.Value
    private static class EvaluationContent {
        List<String> criterios;
        List<String> evidencias;
        InstrumentoEvaluacionDto instrumentoEvaluacion;
    }

    @lombok.Value
    private static class GeneratedSesionContent {
        String titulo;
        String proposito;
        ActividadesSesionDto actividades;
        List<String> criterios;
        List<String> evidencias;
        InstrumentoEvaluacionDto instrumentoEvaluacion;
    }
}
