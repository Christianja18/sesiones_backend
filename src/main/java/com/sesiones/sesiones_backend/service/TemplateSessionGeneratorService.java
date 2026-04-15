package com.sesiones.sesiones_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.dto.TextoReferenciaResponse;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Grado;

@Service
public class TemplateSessionGeneratorService {

    public SesionResponse generate(
        GenerateSesionRequest request,
        Grado grado,
        Area area,
        Competencia competencia,
        List<Capacidad> capacidades,
        List<Desempeno> desempenos
    ) {
        String tema = request.getTema().trim();
        String contexto = request.getContexto().trim();
        String competenciaTexto = competencia.getDescripcion().trim();

        return SesionResponse.builder()
            .titulo("Sesion de " + area.getNombre() + ": " + tema)
            .proposito(buildPurpose(grado.getNombre(), competenciaTexto, tema, contexto))
            .duracionMinutos(request.getDuracionMinutos())
            .generadoPorIa(false)
            .competencias(List.of(new TextoReferenciaResponse(competencia.getId(), competenciaTexto)))
            .capacidades(capacidades.stream().map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion())).toList())
            .desempenos(desempenos.stream().map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion())).toList())
            .actividades(buildActivities(tema, contexto, competenciaTexto))
            .criteriosEvaluacion(buildCriteria(capacidades, desempenos))
            .evidencias(buildEvidence(tema, desempenos))
            .instrumentoEvaluacion(buildInstrument(capacidades, desempenos))
            .build();
    }

    private String buildPurpose(String grado, String competencia, String tema, String contexto) {
        return "Que los estudiantes de " + grado
            + " desarrollen la competencia \"" + competencia + "\""
            + " mediante actividades vinculadas al tema \"" + tema + "\""
            + " y conectadas con el contexto: " + contexto + ".";
    }

    private ActividadesSesionDto buildActivities(String tema, String contexto, String competencia) {
        return ActividadesSesionDto.builder()
            .inicio(List.of(
                "Activar saberes previos con preguntas guiadas sobre " + tema + ".",
                "Presentar el propÃƒÂ³sito de aprendizaje y relacionarlo con el contexto: " + contexto + "."
            ))
            .desarrollo(List.of(
                "Desarrollar una actividad central que permita evidenciar la competencia: " + competencia + ".",
                "AcompaÃƒÂ±ar el trabajo con retroalimentaciÃƒÂ³n formativa y ejemplos contextualizados."
            ))
            .cierre(List.of(
                "Socializar hallazgos y recoger conclusiones del grupo.",
                "Promover metacogniciÃƒÂ³n con una reflexiÃƒÂ³n breve sobre lo aprendido y su aplicaciÃƒÂ³n."
            ))
            .build();
    }

    private List<String> buildCriteria(List<Capacidad> capacidades, List<Desempeno> desempenos) {
        if (!desempenos.isEmpty()) {
            return desempenos.stream()
                .map(item -> "EvalÃƒÂºa evidencias alineadas al desempeÃƒÂ±o: " + item.getDescripcion())
                .toList();
        }

        return capacidades.stream()
            .map(item -> "Verifica avances en la capacidad: " + item.getDescripcion())
            .toList();
    }

    private List<String> buildEvidence(String tema, List<Desempeno> desempenos) {
        if (!desempenos.isEmpty()) {
            return desempenos.stream()
                .map(item -> "Producto o actuaciÃƒÂ³n observable vinculada a: " + item.getDescripcion())
                .toList();
        }

        return List.of("Registro breve de aprendizaje aplicado al tema: " + tema + ".");
    }

    private InstrumentoEvaluacionDto buildInstrument(List<Capacidad> capacidades, List<Desempeno> desempenos) {
        List<String> detail = !desempenos.isEmpty()
            ? desempenos.stream().map(item -> "Indicador: " + item.getDescripcion()).toList()
            : capacidades.stream().map(item -> "Indicador: " + item.getDescripcion()).toList();

        return InstrumentoEvaluacionDto.builder()
            .tipo(InstrumentoTipo.RUBRICA.getDatabaseValue())
            .detalle(detail)
            .build();
    }
}

