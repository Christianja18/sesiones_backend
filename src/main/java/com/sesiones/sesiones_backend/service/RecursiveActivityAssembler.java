package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.util.enums.ActividadTipo;
import com.sesiones.sesiones_backend.entity.Actividad;
import com.sesiones.sesiones_backend.entity.Sesion;

@Service
public class RecursiveActivityAssembler {

    public List<Actividad> assemble(Sesion sesion, ActividadesSesionDto actividadesDto) {
        List<Actividad> actividades = new ArrayList<>();
        List<ActivitySeed> seeds = List.of(
            new ActivitySeed(ActividadTipo.INICIO, safeList(actividadesDto.getInicio())),
            new ActivitySeed(ActividadTipo.DESARROLLO, safeList(actividadesDto.getDesarrollo())),
            new ActivitySeed(ActividadTipo.CIERRE, safeList(actividadesDto.getCierre()))
        );
        appendGroupRecursively(actividades, seeds, 0, 1, sesion);
        return actividades;
    }

    private int appendGroupRecursively(
        List<Actividad> target,
        List<ActivitySeed> seeds,
        int groupIndex,
        int nextOrder,
        Sesion sesion
    ) {
        if (groupIndex >= seeds.size()) {
            return nextOrder;
        }

        ActivitySeed seed = seeds.get(groupIndex);
        int updatedOrder = appendItemsRecursively(target, seed.tipo(), seed.descripciones(), 0, nextOrder, sesion);
        return appendGroupRecursively(target, seeds, groupIndex + 1, updatedOrder, sesion);
    }

    private int appendItemsRecursively(
        List<Actividad> target,
        ActividadTipo tipo,
        List<String> descripciones,
        int itemIndex,
        int nextOrder,
        Sesion sesion
    ) {
        if (itemIndex >= descripciones.size()) {
            return nextOrder;
        }

        Actividad actividad = new Actividad();
        actividad.setSesion(sesion);
        actividad.setTipo(tipo);
        actividad.setDescripcion(descripciones.get(itemIndex).trim());
        actividad.setOrden(nextOrder);
        target.add(actividad);

        return appendItemsRecursively(target, tipo, descripciones, itemIndex + 1, nextOrder + 1, sesion);
    }

    private List<String> safeList(List<String> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
            .filter(item -> item != null && !item.isBlank())
            .toList();
    }

    private record ActivitySeed(ActividadTipo tipo, List<String> descripciones) {
    }
}

