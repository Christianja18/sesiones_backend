package com.sesiones.sesiones_backend.service;

import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Grado;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResultadoPoblacionCurricular {

    private final Area area;
    private final Grado grado;
}
