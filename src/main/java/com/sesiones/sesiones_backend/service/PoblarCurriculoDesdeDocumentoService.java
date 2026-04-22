package com.sesiones.sesiones_backend.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoblarCurriculoDesdeDocumentoService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final CurriculoLlmClient curriculoLlmClient;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final GradoRepository gradoRepository;
    private final AreaRepository areaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CapacidadRepository capacidadRepository;
    private final DesempenoRepository desempenoRepository;

    public ResultadoPoblacionCurricular execute(byte[] archivoPdf, Area areaSugerida, Grado gradoSugerido) {
        String textoPdf = pdfTextExtractorService.extractText(archivoPdf);
        String nivelSugerido = gradoSugerido == null || gradoSugerido.getNivel() == null ? null : gradoSugerido.getNivel().getNombre();
        String gradoNombreSugerido = gradoSugerido == null ? null : gradoSugerido.getNombre();
        String areaNombreSugerida = areaSugerida == null ? null : areaSugerida.getNombre();

        CurriculoDocumentoParseResponse parsed = curriculoLlmClient.extraerCurriculo(
            textoPdf,
            areaNombreSugerida,
            gradoNombreSugerido,
            nivelSugerido
        );

        if (parsed.getNiveles() == null || parsed.getNiveles().isEmpty()) {
            throw new BusinessRuleException("No fue posible identificar niveles y grados en el PDF curricular");
        }

        Set<Area> areasEncontradas = new LinkedHashSet<>();
        Set<Grado> gradosEncontrados = new LinkedHashSet<>();

        for (CurriculoDocumentoParseResponse.NivelItem nivelItem : parsed.getNiveles()) {
            String nivelNombre = normalizeText(nivelItem.getNombre());
            if (nivelNombre == null) {
                continue;
            }

            NivelEducativo nivel = findOrCreateNivel(nivelNombre);
            if (nivelItem.getGrados() == null) {
                continue;
            }

            for (CurriculoDocumentoParseResponse.GradoItem gradoItem : nivelItem.getGrados()) {
                String gradoNombre = normalizeText(gradoItem.getNombre());
                if (gradoNombre == null) {
                    continue;
                }

                Grado grado = findOrCreateGrado(nivel, gradoNombre);
                gradosEncontrados.add(grado);

                if (gradoItem.getAreas() == null) {
                    continue;
                }

                for (CurriculoDocumentoParseResponse.AreaItem areaItem : gradoItem.getAreas()) {
                    String areaNombre = normalizeText(areaItem.getNombre());
                    if (areaNombre == null) {
                        continue;
                    }

                    Area area = findOrCreateArea(areaNombre);
                    areasEncontradas.add(area);

                    if (areaItem.getCompetencias() == null) {
                        continue;
                    }

                    for (CurriculoDocumentoParseResponse.CompetenciaItem competenciaItem : areaItem.getCompetencias()) {
                        String competenciaDescripcion = normalizeText(competenciaItem.getDescripcion());
                        if (competenciaDescripcion == null) {
                            continue;
                        }

                        Competencia competencia = findOrCreateCompetencia(area, competenciaDescripcion);

                        if (competenciaItem.getCapacidades() != null) {
                            for (String capacidadDescripcion : new LinkedHashSet<>(competenciaItem.getCapacidades())) {
                                String normalizedCapacidad = normalizeText(capacidadDescripcion);
                                if (normalizedCapacidad != null) {
                                    findOrCreateCapacidad(competencia, normalizedCapacidad);
                                }
                            }
                        }

                        if (competenciaItem.getDesempenos() != null) {
                            for (String desempenoDescripcion : new LinkedHashSet<>(competenciaItem.getDesempenos())) {
                                String normalizedDesempeno = normalizeText(desempenoDescripcion);
                                if (normalizedDesempeno != null) {
                                    findOrCreateDesempeno(grado, competencia, normalizedDesempeno);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (areasEncontradas.isEmpty() && areaSugerida == null) {
            throw new BusinessRuleException("No fue posible identificar areas curriculares en el PDF");
        }

        if (gradosEncontrados.isEmpty() && gradoSugerido == null) {
            throw new BusinessRuleException("No fue posible identificar grados curriculares en el PDF");
        }

        Area areaFinal = areaSugerida != null ? areaSugerida : singleOrNull(areasEncontradas);
        Grado gradoFinal = gradoSugerido != null ? gradoSugerido : singleOrNull(gradosEncontrados);
        return new ResultadoPoblacionCurricular(areaFinal, gradoFinal);
    }

    private NivelEducativo findOrCreateNivel(String nombre) {
        return nivelEducativoRepository.findByNombreIgnoreCase(nombre)
            .orElseGet(() -> {
                NivelEducativo nivel = new NivelEducativo();
                nivel.setNombre(nombre);
                return nivelEducativoRepository.save(nivel);
            });
    }

    private Grado findOrCreateGrado(NivelEducativo nivel, String nombre) {
        return gradoRepository.findByNivelIdAndNombreIgnoreCase(nivel.getId(), nombre)
            .orElseGet(() -> {
                Grado grado = new Grado();
                grado.setNivel(nivel);
                grado.setNombre(nombre);
                return gradoRepository.save(grado);
            });
    }

    private Area findOrCreateArea(String nombre) {
        return areaRepository.findByNombreIgnoreCase(nombre)
            .orElseGet(() -> {
                Area area = new Area();
                area.setNombre(nombre);
                return areaRepository.save(area);
            });
    }

    private Competencia findOrCreateCompetencia(Area area, String descripcion) {
        return competenciaRepository.findByAreaIdAndDescripcion(area.getId(), descripcion)
            .orElseGet(() -> {
                Competencia competencia = new Competencia();
                competencia.setArea(area);
                competencia.setDescripcion(descripcion);
                return competenciaRepository.save(competencia);
            });
    }

    private Capacidad findOrCreateCapacidad(Competencia competencia, String descripcion) {
        return capacidadRepository.findByCompetenciaIdAndDescripcion(competencia.getId(), descripcion)
            .orElseGet(() -> {
                Capacidad capacidad = new Capacidad();
                capacidad.setCompetencia(competencia);
                capacidad.setDescripcion(descripcion);
                return capacidadRepository.save(capacidad);
            });
    }

    private Desempeno findOrCreateDesempeno(Grado grado, Competencia competencia, String descripcion) {
        return desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcion(grado.getId(), competencia.getId(), descripcion)
            .orElseGet(() -> {
                Desempeno desempeno = new Desempeno();
                desempeno.setGrado(grado);
                desempeno.setCompetencia(competencia);
                desempeno.setDescripcion(descripcion);
                return desempenoRepository.save(desempeno);
            });
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private <T> T singleOrNull(Set<T> items) {
        if (items.size() == 1) {
            return items.iterator().next();
        }
        return null;
    }
}
