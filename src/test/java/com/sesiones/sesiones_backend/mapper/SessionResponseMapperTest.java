package com.sesiones.sesiones_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.entity.Actividad;
import com.sesiones.sesiones_backend.entity.InstrumentoEvaluacion;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.util.enums.ActividadTipo;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

class SessionResponseMapperTest {

    private final SessionResponseMapper mapper = new SessionResponseMapper(new ObjectMapper());

    @Test
    void shouldKeepActivityOrderWhenMappingSessionResponse() {
        Sesion sesion = new Sesion();
        sesion.setActividades(Arrays.asList(
            buildActividad(ActividadTipo.DESARROLLO, "Segundo paso", 3),
            buildActividad(ActividadTipo.INICIO, "Primer paso", 1),
            buildActividad(ActividadTipo.DESARROLLO, "Paso intermedio", 2),
            buildActividad(ActividadTipo.CIERRE, "Cierre final", 4)
        ));

        var response = mapper.toSesionResponse(sesion);

        assertEquals(Collections.singletonList("Primer paso"), response.getActividades().getInicio());
        assertEquals(Arrays.asList("Paso intermedio", "Segundo paso"), response.getActividades().getDesarrollo());
        assertEquals(Collections.singletonList("Cierre final"), response.getActividades().getCierre());
    }

    @Test
    void shouldReadInstrumentTypeFromColumnAndDetailFromJson() {
        Sesion sesion = new Sesion();
        InstrumentoEvaluacion instrumento = new InstrumentoEvaluacion();
        instrumento.setTipo(InstrumentoTipo.RUBRICA);
        instrumento.setContenidoJson("{\"detalle\":[\"Claridad\",\"Coherencia\"]}");
        sesion.setInstrumentosEvaluacion(Collections.singletonList(instrumento));

        var response = mapper.toSesionResponse(sesion);

        assertEquals("rubrica", response.getInstrumentoEvaluacion().getTipo());
        assertEquals(Arrays.asList("Claridad", "Coherencia"), response.getInstrumentoEvaluacion().getDetalle());
    }

    @Test
    void shouldRejectInvalidInstrumentTypeWhenCreatingJson() {
        InstrumentoEvaluacionDto dto = InstrumentoEvaluacionDto.builder()
            .tipo("checklist")
            .detalle(Collections.singletonList("Uno"))
            .build();

        assertThrows(BusinessRuleException.class, () -> mapper.toInstrumentJson(dto));
    }

    private Actividad buildActividad(ActividadTipo tipo, String descripcion, Integer orden) {
        Actividad actividad = new Actividad();
        actividad.setTipo(tipo);
        actividad.setDescripcion(descripcion);
        actividad.setOrden(orden);
        return actividad;
    }
}
