package com.sesiones.sesiones_backend.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoblarCurriculoDesdeDocumentoService {

    private final ReferenceResolver referenceResolver;
    private final AreaRepository areaRepository;
    private final GradoRepository gradoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CapacidadRepository capacidadRepository;
    private final DesempenoRepository desempenoRepository;
    private final PdfDocumentoCurriculoExtractorService pdfDocumentoCurriculoExtractorService;

    @Transactional
    public ResultadoPoblacion execute(byte[] archivoPdf, Integer areaId, Integer gradoId) {
        ExtraccionDocumento extraccion = extractDocumentData(archivoPdf);
        Area area = resolveArea(areaId, extraccion);
        Grado grado = resolveGrado(gradoId, extraccion);

        if (extraccion.getCompetencias().isEmpty()) {
            throw new BusinessRuleException(
                "No se encontraron competencias en el PDF. Usa etiquetas como Competencia:, Capacidad: y Desempeno:."
            );
        }

        boolean contieneDesempenos = extraccion.getCompetencias().stream()
            .anyMatch(item -> !item.getDesempenos().isEmpty());
        if (contieneDesempenos && grado == null) {
            throw new BusinessRuleException(
                "No se pudo identificar el grado del documento. Envia gradoId o incluye Nivel: y Grado: en el PDF."
            );
        }

        for (CompetenciaExtraida competenciaExtraida : extraccion.getCompetencias()) {
            Competencia competencia = competenciaRepository
                .findByAreaIdAndDescripcionIgnoreCase(area.getId(), competenciaExtraida.getDescripcion())
                .orElseGet(() -> saveCompetencia(area, competenciaExtraida.getDescripcion()));

            for (String capacidadDescripcion : competenciaExtraida.getCapacidades()) {
                capacidadRepository
                    .findByCompetenciaIdAndDescripcionIgnoreCase(competencia.getId(), capacidadDescripcion)
                    .orElseGet(() -> saveCapacidad(competencia, capacidadDescripcion));
            }

            if (grado != null) {
                for (String desempenoDescripcion : competenciaExtraida.getDesempenos()) {
                    desempenoRepository
                        .findByGradoIdAndCompetenciaIdAndDescripcionIgnoreCase(
                            grado.getId(),
                            competencia.getId(),
                            desempenoDescripcion
                        )
                        .orElseGet(() -> saveDesempeno(grado, competencia, desempenoDescripcion));
                }
            }
        }

        return new ResultadoPoblacion(area, grado);
    }

    private ExtraccionDocumento extractDocumentData(byte[] archivoPdf) {
        String rawText = pdfDocumentoCurriculoExtractorService.extractText(archivoPdf);
        String[] lines = rawText.split("\\R");

        ExtraccionDocumento extraccion = new ExtraccionDocumento();
        SeccionActual seccionActual = SeccionActual.NINGUNA;
        CompetenciaExtraida competenciaActual = null;

        for (String line : lines) {
            String cleanedLine = cleanLine(line);
            if (cleanedLine.isBlank()) {
                continue;
            }

            String normalizedLine = normalize(cleanedLine);
            extraccion.appendTextoCompleto(cleanedLine);

            if (normalizedLine.startsWith("nivel:")) {
                extraccion.setNivelNombre(extractValue(cleanedLine));
                seccionActual = SeccionActual.NINGUNA;
                continue;
            }
            if (normalizedLine.startsWith("grado:")) {
                extraccion.setGradoNombre(extractValue(cleanedLine));
                seccionActual = SeccionActual.NINGUNA;
                continue;
            }
            if (normalizedLine.startsWith("area:")) {
                extraccion.setAreaNombre(extractValue(cleanedLine));
                seccionActual = SeccionActual.NINGUNA;
                continue;
            }
            if (isHeading(normalizedLine, "competencia", "competencias")) {
                seccionActual = SeccionActual.COMPETENCIA;
                continue;
            }
            if (isHeading(normalizedLine, "capacidad", "capacidades")) {
                seccionActual = SeccionActual.CAPACIDAD;
                continue;
            }
            if (isHeading(normalizedLine, "desempeno", "desempenos")) {
                seccionActual = SeccionActual.DESEMPENO;
                continue;
            }

            if (normalizedLine.startsWith("competencia:") || normalizedLine.startsWith("competencias:")) {
                competenciaActual = extraccion.addCompetencia(extractValue(cleanedLine));
                seccionActual = SeccionActual.COMPETENCIA;
                continue;
            }
            if (normalizedLine.startsWith("capacidad:") || normalizedLine.startsWith("capacidades:")) {
                if (competenciaActual != null) {
                    competenciaActual.addCapacidad(extractValue(cleanedLine));
                }
                seccionActual = SeccionActual.CAPACIDAD;
                continue;
            }
            if (normalizedLine.startsWith("desempeno:") || normalizedLine.startsWith("desempenos:")) {
                if (competenciaActual != null) {
                    competenciaActual.addDesempeno(extractValue(cleanedLine));
                }
                seccionActual = SeccionActual.DESEMPENO;
                continue;
            }

            if (seccionActual == SeccionActual.COMPETENCIA) {
                competenciaActual = extraccion.addCompetencia(cleanedLine);
                continue;
            }
            if (seccionActual == SeccionActual.CAPACIDAD && competenciaActual != null) {
                competenciaActual.addCapacidad(cleanedLine);
                continue;
            }
            if (seccionActual == SeccionActual.DESEMPENO && competenciaActual != null) {
                competenciaActual.addDesempeno(cleanedLine);
            }
        }

        return extraccion;
    }

    private Area resolveArea(Integer areaId, ExtraccionDocumento extraccion) {
        if (areaId != null) {
            return referenceResolver.findArea(areaId);
        }

        String areaNombre = extraccion.getAreaNombre();
        if (areaNombre == null || areaNombre.isBlank()) {
            List<Area> areas = areaRepository.findAll();
            List<Area> coincidencias = new ArrayList<>();
            String normalizedText = normalize(extraccion.getTextoCompleto());
            for (Area area : areas) {
                if (normalizedText.contains(normalize(area.getNombre()))) {
                    coincidencias.add(area);
                }
            }
            if (coincidencias.size() == 1) {
                return coincidencias.get(0);
            }
            throw new BusinessRuleException(
                "No se pudo identificar el area del documento. Envia areaId o incluye Area: en el PDF."
            );
        }

        return areaRepository.findByNombreIgnoreCase(areaNombre)
            .orElseGet(() -> {
                Area area = new Area();
                area.setNombre(areaNombre);
                return areaRepository.save(area);
            });
    }

    private Grado resolveGrado(Integer gradoId, ExtraccionDocumento extraccion) {
        if (gradoId != null) {
            return referenceResolver.findGrado(gradoId);
        }

        String gradoNombre = extraccion.getGradoNombre();
        if (gradoNombre == null || gradoNombre.isBlank()) {
            return null;
        }

        String nivelNombre = extraccion.getNivelNombre();
        if (nivelNombre != null && !nivelNombre.isBlank()) {
            return gradoRepository.findByNombreIgnoreCaseAndNivelNombreIgnoreCase(gradoNombre, nivelNombre)
                .orElseThrow(() -> new BusinessRuleException(
                    "No existe un grado registrado para el nivel identificado en el documento."
                ));
        }

        List<Grado> grados = gradoRepository.findByNombreIgnoreCase(gradoNombre);
        if (grados.size() == 1) {
            return grados.get(0);
        }
        return null;
    }

    private Competencia saveCompetencia(Area area, String descripcion) {
        Competencia competencia = new Competencia();
        competencia.setArea(area);
        competencia.setDescripcion(descripcion);
        return competenciaRepository.save(competencia);
    }

    private Capacidad saveCapacidad(Competencia competencia, String descripcion) {
        Capacidad capacidad = new Capacidad();
        capacidad.setCompetencia(competencia);
        capacidad.setDescripcion(descripcion);
        return capacidadRepository.save(capacidad);
    }

    private Desempeno saveDesempeno(Grado grado, Competencia competencia, String descripcion) {
        Desempeno desempeno = new Desempeno();
        desempeno.setGrado(grado);
        desempeno.setCompetencia(competencia);
        desempeno.setDescripcion(descripcion);
        return desempenoRepository.save(desempeno);
    }

    private boolean isHeading(String normalizedLine, String singular, String plural) {
        return normalizedLine.equals(singular) || normalizedLine.equals(plural);
    }

    private String extractValue(String line) {
        int separatorIndex = line.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == line.length() - 1) {
            return "";
        }
        return line.substring(separatorIndex + 1).trim();
    }

    private String cleanLine(String line) {
        String sanitized = line == null ? "" : line.replace('\u00A0', ' ').trim();
        sanitized = sanitized.replaceFirst("^[\\-\\*\\u2022\\u00B7]+\\s*", "");
        sanitized = sanitized.replaceFirst("^\\d+[\\.)-]\\s*", "");
        return sanitized.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
    }

    public enum SeccionActual {
        NINGUNA,
        COMPETENCIA,
        CAPACIDAD,
        DESEMPENO
    }

    @Getter
    public static class ResultadoPoblacion {

        private final Area area;
        private final Grado grado;

        public ResultadoPoblacion(Area area, Grado grado) {
            this.area = area;
            this.grado = grado;
        }
    }

    private static class ExtraccionDocumento {

        private final List<CompetenciaExtraida> competencias = new ArrayList<>();
        private final StringBuilder textoCompleto = new StringBuilder();
        private String areaNombre;
        private String nivelNombre;
        private String gradoNombre;

        public CompetenciaExtraida addCompetencia(String descripcion) {
            CompetenciaExtraida competencia = new CompetenciaExtraida(descripcion);
            if (!competencia.getDescripcion().isBlank()) {
                competencias.add(competencia);
                return competencia;
            }
            return null;
        }

        public void appendTextoCompleto(String linea) {
            if (textoCompleto.length() > 0) {
                textoCompleto.append(' ');
            }
            textoCompleto.append(linea);
        }

        public List<CompetenciaExtraida> getCompetencias() {
            return competencias;
        }

        public String getTextoCompleto() {
            return textoCompleto.toString();
        }

        public String getAreaNombre() {
            return areaNombre;
        }

        public void setAreaNombre(String areaNombre) {
            this.areaNombre = areaNombre;
        }

        public String getNivelNombre() {
            return nivelNombre;
        }

        public void setNivelNombre(String nivelNombre) {
            this.nivelNombre = nivelNombre;
        }

        public String getGradoNombre() {
            return gradoNombre;
        }

        public void setGradoNombre(String gradoNombre) {
            this.gradoNombre = gradoNombre;
        }
    }

    private static class CompetenciaExtraida {

        private final String descripcion;
        private final Set<String> capacidades = new LinkedHashSet<>();
        private final Set<String> desempenos = new LinkedHashSet<>();

        public CompetenciaExtraida(String descripcion) {
            this.descripcion = descripcion == null ? "" : descripcion.trim();
        }

        public String getDescripcion() {
            return descripcion;
        }

        public Set<String> getCapacidades() {
            return capacidades;
        }

        public Set<String> getDesempenos() {
            return desempenos;
        }

        public void addCapacidad(String descripcionCapacidad) {
            String valor = descripcionCapacidad == null ? "" : descripcionCapacidad.trim();
            if (!valor.isBlank()) {
                capacidades.add(valor);
            }
        }

        public void addDesempeno(String descripcionDesempeno) {
            String valor = descripcionDesempeno == null ? "" : descripcionDesempeno.trim();
            if (!valor.isBlank()) {
                desempenos.add(valor);
            }
        }
    }
}
