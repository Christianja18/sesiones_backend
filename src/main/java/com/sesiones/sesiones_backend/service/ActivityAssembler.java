package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.util.enums.ActividadTipo;
import com.sesiones.sesiones_backend.entity.Actividad;
import com.sesiones.sesiones_backend.entity.Sesion;

@Service
public class ActivityAssembler {

    public List<Actividad> assemble(Sesion sesion, ActividadesSesionDto actividadesDto) {
        List<Actividad> actividades = new ArrayList<>();
        List<ActivitySeed> seeds = List.of(
            new ActivitySeed(ActividadTipo.INICIO, safeList(actividadesDto.getInicio())),
            new ActivitySeed(ActividadTipo.DESARROLLO, safeList(actividadesDto.getDesarrollo())),
            new ActivitySeed(ActividadTipo.CIERRE, safeList(actividadesDto.getCierre()))
        );

        int orden = 1;
        for (ActivitySeed seed : seeds) {
            for (String descripcion : seed.getDescripciones()) {
                actividades.add(buildActividad(sesion, seed.getTipo(), descripcion, orden++));
            }
        }

        return actividades;
    }

    private Actividad buildActividad(Sesion sesion, ActividadTipo tipo, String descripcion, int orden) {
        Actividad actividad = new Actividad();
        actividad.setSesion(sesion);
        actividad.setTipo(tipo);
        actividad.setDescripcion(descripcion.trim());
        actividad.setOrden(orden);
        return actividad;
    }

    private List<String> safeList(List<String> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
            .filter(item -> item != null && !item.isBlank())
            .collect(Collectors.toList());
    }

    private static final class ActivitySeed {

        private final ActividadTipo tipo;
        private final List<String> descripciones;

        private ActivitySeed(ActividadTipo tipo, List<String> descripciones) {
            this.tipo = tipo;
            this.descripciones = descripciones;
        }

        private ActividadTipo getTipo() {
            return tipo;
        }

        private List<String> getDescripciones() {
            return descripciones;
        }
    }
}

