package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import com.sesiones.sesiones_backend.entity.Ciclo;
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
    private static final String[] ALTERNATIVE_FOCUSES = {
        "resolucion de problemas contextualizados",
        "trabajo colaborativo con producto observable",
        "indagacion guiada con preguntas retadoras",
        "aplicacion practica en situaciones del entorno",
        "analisis de evidencias y argumentacion"
    };

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
        List<Competencia> competencias = referenceResolver.findCompetenciasByArea(request.selectedCompetenciaIds(), request.getAreaId());
        List<Capacidad> capacidades = competencias.stream()
            .flatMap(competencia -> capacidadRepository.findByCompetenciaIdOrderByIdAsc(competencia.getId()).stream())
            .toList();
        String cicloId = requireCicloId(grado);
        List<EstandarAprendizaje> estandares = competencias.stream()
            .flatMap(competencia -> estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(
                competencia.getId(),
                cicloId
            ).stream())
            .toList();
        List<Desempeno> desempenos = competencias.stream()
            .flatMap(competencia -> desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(
                grado.getId(),
                competencia.getId()
            ).stream())
            .toList();
        String alternativeKey = buildAlternativeKey(request);
        String alternativeFocus = selectAlternativeFocus(alternativeKey);

        GeneratedSesionContent generatedContent = generateWithRetry(
            request,
            nivel,
            grado,
            area,
            competencias,
            capacidades,
            estandares,
            desempenos,
            alternativeKey,
            alternativeFocus
        );
        if (generatedContent == null) {
            return templateSessionGeneratorService.generate(request, grado, area, competencias, capacidades, estandares, desempenos);
        }

        return SesionResponse.builder()
            .titulo(generatedContent.getTitulo())
            .proposito(generatedContent.getProposito())
            .duracionMinutos(request.getDuracionMinutos())
            .generadoPorIa(true)
            .competencias(toReferenceResponses(competencias))
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
        List<Competencia> competencias,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus
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
                    competencias,
                    capacidades,
                    estandares,
                    desempenos,
                    alternativeKey,
                    alternativeFocus,
                    strictRetry
                );
                ActividadesSesionDto activities = generateActivities(
                    request,
                    nivel,
                    grado,
                    area,
                    competencias,
                    structure,
                    capacidades,
                    estandares,
                    desempenos,
                    alternativeKey,
                    alternativeFocus,
                    strictRetry
                );
                EvaluationContent evaluation = generateEvaluation(
                    request,
                    nivel,
                    grado,
                    area,
                    competencias,
                    structure,
                    capacidades,
                    estandares,
                    desempenos,
                    alternativeKey,
                    alternativeFocus,
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
        List<Competencia> competencias,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
        boolean strictRetry
    ) {
        String prompt = buildStructurePrompt(
            request,
            nivel,
            grado,
            area,
            competencias,
            capacidades,
            estandares,
            desempenos,
            alternativeKey,
            alternativeFocus,
            strictRetry
        );
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
        List<Competencia> competencias,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
        boolean strictRetry
    ) {
        String prompt = buildActivitiesPrompt(
            request,
            nivel,
            grado,
            area,
            competencias,
            structure,
            capacidades,
            estandares,
            desempenos,
            alternativeKey,
            alternativeFocus,
            strictRetry
        );
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
        List<Competencia> competencias,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
        boolean strictRetry
    ) {
        String prompt = buildEvaluationPrompt(
            request,
            nivel,
            grado,
            area,
            competencias,
            structure,
            capacidades,
            estandares,
            desempenos,
            alternativeKey,
            alternativeFocus,
            strictRetry
        );
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
        List<Competencia> competencias,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
        boolean strictRetry
    ) {
        String prompt = """
            Genera una sesion de aprendizaje basada en el Curriculo Nacional del Peru.

            Datos:
            - Nivel: %s
            - Grado: %s
            - Ciclo: %s
            - Area: %s
            - Competencias:
            %s
            - Tema: %s
            - Contexto: %s
            - Duracion: %s minutos
            - Codigo de alternativa: %s
            - Enfoque de esta alternativa: %s
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Reglas de alineacion:
            - Ajusta la complejidad cognitiva, el lenguaje y la autonomia al nivel, grado y ciclo indicados.
            - Mantente estrictamente en el area y competencias indicadas.
            - Usa como base las capacidades, estandares y desempenos de referencia; no inventes otro curriculo.
            - Genera una alternativa distinta para el docente variando situacion, dinamica y producto.

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
            formatCiclo(grado),
            area.getNombre(),
            joinCompetencias(competencias),
            request.getTema().trim(),
            request.getContexto().trim(),
            request.getDuracionMinutos(),
            alternativeKey,
            alternativeFocus,
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
        List<Competencia> competencias,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
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
            - Ciclo: %s
            - Area: %s
            - Competencias:
            %s
            - Tema: %s
            - Contexto: %s
            - Titulo: %s
            - Proposito: %s
            - Codigo de alternativa: %s
            - Enfoque de esta alternativa: %s
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Reglas de alineacion:
            - Ajusta la complejidad de las actividades al nivel, grado y ciclo indicados.
            - Mantente estrictamente en el area y competencias indicadas.
            - Propone una secuencia distinta para esta alternativa.

            Devuelve SOLO en JSON valido:
            {
              "inicio": [],
              "desarrollo": [],
              "cierre": []
            }
            """.formatted(
            nivel.getNombre(),
            grado.getNombre(),
            formatCiclo(grado),
            area.getNombre(),
            joinCompetencias(competencias),
            request.getTema().trim(),
            request.getContexto().trim(),
            structure.getTitulo(),
            structure.getProposito(),
            alternativeKey,
            alternativeFocus,
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
        List<Competencia> competencias,
        StructureContent structure,
        List<Capacidad> capacidades,
        List<EstandarAprendizaje> estandares,
        List<Desempeno> desempenos,
        String alternativeKey,
        String alternativeFocus,
        boolean strictRetry
    ) {
        String prompt = """
            Genera evaluacion formativa alineada a competencias.

            Datos:
            - Nivel: %s
            - Grado: %s
            - Ciclo: %s
            - Area: %s
            - Competencias:
            %s
            - Tema: %s
            - Contexto: %s
            - Titulo: %s
            - Proposito: %s
            - Codigo de alternativa: %s
            - Enfoque de esta alternativa: %s
            - Capacidades de referencia:
            %s
            - Estandares de referencia:
            %s
            - Desempenos de referencia:
            %s

            Reglas de alineacion:
            - Los criterios deben medir las competencias, capacidades y desempenos de este grado.
            - La exigencia debe corresponder al nivel, grado y ciclo indicados.
            - La evidencia e instrumento deben corresponder a la alternativa generada.

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
            formatCiclo(grado),
            area.getNombre(),
            joinCompetencias(competencias),
            request.getTema().trim(),
            request.getContexto().trim(),
            structure.getTitulo(),
            structure.getProposito(),
            alternativeKey,
            alternativeFocus,
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

    private String requireCicloId(Grado grado) {
        if (grado.getCiclo() == null || grado.getCiclo().getId() == null || grado.getCiclo().getId().isBlank()) {
            throw new BusinessRuleException("El grado seleccionado no tiene ciclo educativo configurado");
        }
        return grado.getCiclo().getId();
    }

    private String formatCiclo(Grado grado) {
        Ciclo ciclo = grado.getCiclo();
        String nombre = ciclo.getNombre() == null || ciclo.getNombre().isBlank()
            ? ciclo.getId()
            : ciclo.getNombre();
        return ciclo.getId() + " - " + nombre;
    }

    private String buildAlternativeKey(GenerateSesionRequest request) {
        if (request.getAlternativa() != null) {
            return "alternativa-" + request.getAlternativa();
        }
        return "alternativa-" + UUID.randomUUID();
    }

    private String selectAlternativeFocus(String alternativeKey) {
        int index = Math.floorMod(alternativeKey.hashCode(), ALTERNATIVE_FOCUSES.length);
        return ALTERNATIVE_FOCUSES[index];
    }

    private String joinCapacidades(List<Capacidad> capacidades) {
        if (capacidades == null || capacidades.isEmpty()) {
            return "- no_disponible";
        }
        return capacidades.stream()
            .map(Capacidad::getDescripcion)
            .collect(Collectors.joining(System.lineSeparator() + "- ", "- ", ""));
    }

    private String joinCompetencias(List<Competencia> competencias) {
        if (competencias == null || competencias.isEmpty()) {
            return "- no_disponible";
        }
        return competencias.stream()
            .map(Competencia::getDescripcion)
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

    private List<TextoReferenciaResponse> toReferenceResponses(List<Competencia> competencias) {
        return competencias.stream()
            .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
            .collect(Collectors.toList());
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
