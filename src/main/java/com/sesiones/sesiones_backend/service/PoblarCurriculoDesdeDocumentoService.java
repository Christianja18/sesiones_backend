package com.sesiones.sesiones_backend.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Ciclo;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.DocumentoChunk;
import com.sesiones.sesiones_backend.entity.DocumentoChunkClasificacion;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CicloRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.DocumentoChunkClasificacionRepository;
import com.sesiones.sesiones_backend.repository.DocumentoChunkRepository;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoblarCurriculoDesdeDocumentoService {

    private static final Pattern CICLO_PATTERN = Pattern.compile("\\b(VII|VI|V|IV|III)\\b");

    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final DocumentoChunkRepository documentoChunkRepository;
    private final DocumentoChunkClasificacionRepository documentoChunkClasificacionRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final CicloRepository cicloRepository;
    private final GradoRepository gradoRepository;
    private final AreaRepository areaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CapacidadRepository capacidadRepository;
    private final DesempenoRepository desempenoRepository;

    @Transactional
    public void execute(Integer documentoCurriculoId, String checksumSha256, List<DocumentoChunkAnalizado> analisisChunks) {
        if (analisisChunks == null || analisisChunks.isEmpty()) {
            throw new BusinessRuleException("No existen chunks curriculares para poblar la base de datos");
        }

        DocumentoCurriculo documento = documentoCurriculoRepository.findById(documentoCurriculoId)
            .orElseThrow(() -> new BusinessRuleException("No existe el documento curricular a procesar"));
        DocumentoCurriculoTipo tipoDocumento = documento.getTipo();
        if (tipoDocumento == null) {
            throw new BusinessRuleException("El tipo del documento curricular es obligatorio para poblar la base de datos");
        }

        documentoChunkRepository.deleteByDocumentoId(documentoCurriculoId);

        boolean informacionUtilEncontrada = false;

        for (DocumentoChunkAnalizado analisisChunk : analisisChunks) {
            DocumentoChunkContenido chunkContenido = analisisChunk.chunk();
            DocumentoChunk chunk = new DocumentoChunk();
            chunk.setDocumento(documento);
            chunk.setOrden(chunkContenido.orden());
            chunk.setPaginaInicio(chunkContenido.paginaInicio());
            chunk.setPaginaFin(chunkContenido.paginaFin());
            chunk.setContenido(chunkContenido.contenido());
            chunk.setHashContenido(chunkContenido.hashContenido());
            chunk.setEstado(ProcesamientoEstado.PROCESADO);
            DocumentoChunk savedChunk = documentoChunkRepository.save(chunk);

            List<CurriculoDocumentoParseResponse.Item> items = analisisChunk.analisis() == null
                ? List.of()
                : analisisChunk.analisis().getItems();

            for (CurriculoDocumentoParseResponse.Item item : items) {
                ContextoCurricularResuelto contexto = resolveContext(item, tipoDocumento);
                if (!contexto.hasAnyReference()) {
                    continue;
                }

                saveChunkClasificacion(savedChunk, contexto, item.getConfianza());
                informacionUtilEncontrada = persistCurriculumData(tipoDocumento, contexto, item) || informacionUtilEncontrada;
            }
        }

        if (!informacionUtilEncontrada) {
            throw new BusinessRuleException(buildNoUsefulDataMessage(tipoDocumento));
        }

        documento.setChecksumSha256(checksumSha256);
        documento.setEstado(ProcesamientoEstado.PROCESADO);
        documento.setErrorDetalle(null);
        documento.setFechaProcesado(LocalDateTime.now());
        documentoCurriculoRepository.save(documento);
    }

    private void saveChunkClasificacion(DocumentoChunk chunk, ContextoCurricularResuelto contexto, BigDecimal confianza) {
        DocumentoChunkClasificacion clasificacion = new DocumentoChunkClasificacion();
        clasificacion.setChunk(chunk);
        clasificacion.setArea(contexto.area());
        clasificacion.setGrado(contexto.grado());
        clasificacion.setNivel(contexto.nivel());
        clasificacion.setCiclo(contexto.ciclo());
        clasificacion.setConfianza(confianza);
        documentoChunkClasificacionRepository.save(clasificacion);
    }

    private boolean persistCurriculumData(
        DocumentoCurriculoTipo tipoDocumento,
        ContextoCurricularResuelto contexto,
        CurriculoDocumentoParseResponse.Item item
    ) {
        return switch (tipoDocumento) {
            case CURRICULO -> persistCurriculoNacionalData(contexto, item);
            case PROGRAMA -> persistProgramaCurricularData(contexto, item);
        };
    }

    private boolean persistCurriculoNacionalData(ContextoCurricularResuelto contexto, CurriculoDocumentoParseResponse.Item item) {
        String competenciaDescripcion = normalizeText(item.getCompetencia());
        if (contexto.area() == null || competenciaDescripcion == null) {
            return false;
        }

        Competencia competencia = findOrCreateCompetencia(contexto.area(), competenciaDescripcion);
        boolean persisted = true;

        if (item.getCapacidades() != null) {
            for (String capacidadDescripcion : new LinkedHashSet<>(item.getCapacidades())) {
                String normalizedCapacidad = normalizeText(capacidadDescripcion);
                if (normalizedCapacidad != null) {
                    findOrCreateCapacidad(competencia, normalizedCapacidad);
                    persisted = true;
                }
            }
        }

        return persisted;
    }

    private boolean persistProgramaCurricularData(ContextoCurricularResuelto contexto, CurriculoDocumentoParseResponse.Item item) {
        String competenciaDescripcion = normalizeText(item.getCompetencia());
        if (contexto.area() == null || contexto.grado() == null || competenciaDescripcion == null || item.getDesempenos() == null) {
            return false;
        }

        Competencia competencia = competenciaRepository.findByAreaIdAndDescripcion(contexto.area().getId(), competenciaDescripcion)
            .orElse(null);
        if (competencia == null) {
            return false;
        }

        boolean persisted = false;
        for (String desempenoDescripcion : new LinkedHashSet<>(item.getDesempenos())) {
            String normalizedDesempeno = normalizeText(desempenoDescripcion);
            if (normalizedDesempeno != null) {
                findOrCreateDesempeno(contexto.grado(), competencia, normalizedDesempeno);
                persisted = true;
            }
        }
        return persisted;
    }

    private String buildNoUsefulDataMessage(DocumentoCurriculoTipo tipoDocumento) {
        return switch (tipoDocumento) {
            case CURRICULO -> "No fue posible extraer competencias o capacidades utiles del Curriculo Nacional";
            case PROGRAMA -> "No fue posible extraer desempenos utiles del Programa Curricular. Verifica que el Curriculo Nacional haya sido procesado primero";
        };
    }

    private ContextoCurricularResuelto resolveContext(CurriculoDocumentoParseResponse.Item item, DocumentoCurriculoTipo tipoDocumento) {
        if (item == null) {
            return ContextoCurricularResuelto.empty();
        }

        String cicloId = normalizeCicloId(item.getCiclo());
        String nivelNombre = normalizeText(item.getNivel());
        String gradoNombre = normalizeGradeName(item.getGrado());
        String areaNombre = normalizeText(item.getArea());

        boolean onlyExistingCatalog = tipoDocumento == DocumentoCurriculoTipo.PROGRAMA;
        NivelEducativo nivel = onlyExistingCatalog ? resolveExistingNivel(nivelNombre, cicloId) : resolveNivel(nivelNombre, cicloId);
        Ciclo ciclo = onlyExistingCatalog ? resolveExistingCiclo(cicloId) : resolveCiclo(cicloId);
        Grado grado = onlyExistingCatalog
            ? resolveExistingGrado(nivel, ciclo, gradoNombre)
            : resolveGrado(nivel, ciclo, gradoNombre);
        if (grado != null) {
            nivel = grado.getNivel();
            ciclo = grado.getCiclo();
        }

        Area area = onlyExistingCatalog ? resolveExistingArea(areaNombre) : resolveArea(areaNombre);
        return new ContextoCurricularResuelto(area, nivel, ciclo, grado);
    }

    private NivelEducativo resolveNivel(String nivelNombre, String cicloId) {
        String nivelResuelto = normalizeNivelName(nivelNombre);
        if (nivelResuelto == null) {
            nivelResuelto = inferNivelFromCiclo(cicloId);
        }
        if (nivelResuelto == null) {
            return null;
        }
        final String canonicalNivel = nivelResuelto;

        return nivelEducativoRepository.findAll().stream()
            .filter(item -> sameComparableText(item.getNombre(), canonicalNivel))
            .findFirst()
            .orElseGet(() -> {
                NivelEducativo nivel = new NivelEducativo();
                nivel.setNombre(toTitleCase(canonicalNivel));
                return nivelEducativoRepository.save(nivel);
            });
    }

    private NivelEducativo resolveExistingNivel(String nivelNombre, String cicloId) {
        String nivelResuelto = normalizeNivelName(nivelNombre);
        if (nivelResuelto == null) {
            nivelResuelto = inferNivelFromCiclo(cicloId);
        }
        if (nivelResuelto == null) {
            return null;
        }
        final String canonicalNivel = nivelResuelto;

        return nivelEducativoRepository.findAll().stream()
            .filter(item -> sameComparableText(item.getNombre(), canonicalNivel))
            .findFirst()
            .orElse(null);
    }

    private Ciclo resolveCiclo(String cicloId) {
        if (cicloId == null) {
            return null;
        }

        return cicloRepository.findById(cicloId)
            .orElseGet(() -> {
                Ciclo ciclo = new Ciclo();
                ciclo.setId(cicloId);
                ciclo.setNombre("Ciclo " + cicloId);
                return cicloRepository.save(ciclo);
            });
    }

    private Ciclo resolveExistingCiclo(String cicloId) {
        if (cicloId == null) {
            return null;
        }

        return cicloRepository.findById(cicloId).orElse(null);
    }

    private Grado resolveGrado(NivelEducativo nivel, Ciclo ciclo, String gradoNombre) {
        if (gradoNombre == null) {
            return null;
        }

        NivelEducativo nivelResuelto = nivel;
        if (nivelResuelto == null && ciclo != null) {
            nivelResuelto = resolveNivel(null, ciclo.getId());
        }
        if (nivelResuelto == null) {
            return null;
        }

        Grado existing = gradoRepository.findByNivelIdAndNombreIgnoreCase(nivelResuelto.getId(), gradoNombre)
            .orElse(null);
        if (existing == null) {
            return null;
        }

        String cicloEsperado = ciclo != null ? ciclo.getId() : inferCicloFromGrado(nivelResuelto.getNombre(), gradoNombre);
        if (cicloEsperado != null && (existing.getCiclo() == null || !cicloEsperado.equalsIgnoreCase(existing.getCiclo().getId()))) {
            return null;
        }

        return existing;
    }

    private Grado resolveExistingGrado(NivelEducativo nivel, Ciclo ciclo, String gradoNombre) {
        if (gradoNombre == null) {
            return null;
        }

        NivelEducativo nivelResuelto = nivel;
        if (nivelResuelto == null && ciclo != null) {
            nivelResuelto = resolveExistingNivel(null, ciclo.getId());
        }
        if (nivelResuelto == null) {
            return null;
        }

        Grado existing = gradoRepository.findByNivelIdAndNombreIgnoreCase(nivelResuelto.getId(), gradoNombre)
            .orElse(null);
        if (existing == null) {
            return null;
        }

        String cicloEsperado = ciclo != null ? ciclo.getId() : inferCicloFromGrado(nivelResuelto.getNombre(), gradoNombre);
        if (cicloEsperado != null && (existing.getCiclo() == null || !cicloEsperado.equalsIgnoreCase(existing.getCiclo().getId()))) {
            return null;
        }

        return existing;
    }

    private Area resolveArea(String areaNombre) {
        if (areaNombre == null) {
            return null;
        }

        return areaRepository.findAll().stream()
            .filter(item -> sameComparableText(item.getNombre(), areaNombre))
            .findFirst()
            .orElseGet(() -> {
                Area area = new Area();
                area.setNombre(toTitleCase(areaNombre));
                return areaRepository.save(area);
            });
    }

    private Area resolveExistingArea(String areaNombre) {
        if (areaNombre == null) {
            return null;
        }

        return areaRepository.findAll().stream()
            .filter(item -> sameComparableText(item.getNombre(), areaNombre))
            .findFirst()
            .orElse(null);
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

    private String inferNivelFromCiclo(String cicloId) {
        if (cicloId == null) {
            return null;
        }

        return switch (cicloId) {
            case "III", "IV", "V" -> "Primaria";
            case "VI", "VII" -> "Secundaria";
            default -> null;
        };
    }

    private String normalizeNivelName(String value) {
        String normalized = normalizeComparableText(value);
        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case "inicial", "educacion inicial", "nivel inicial" -> "Inicial";
            case "primaria", "educacion primaria", "educacion basica primaria", "primario" -> "Primaria";
            case "secundaria", "educacion secundaria", "educacion basica secundaria", "secundario" -> "Secundaria";
            default -> null;
        };
    }

    private String inferCicloFromGrado(String nivelNombre, String gradoNombre) {
        if (nivelNombre == null || gradoNombre == null) {
            return null;
        }

        String nivel = normalizeComparableText(nivelNombre);
        return switch (gradoNombre) {
            case "1ro", "2do" -> "secundaria".equals(nivel) ? "VI" : "III";
            case "3ero", "4to" -> "secundaria".equals(nivel) ? "VII" : "IV";
            case "5to" -> "secundaria".equals(nivel) ? "VII" : "V";
            case "6to" -> "V";
            default -> null;
        };
    }

    private String normalizeCicloId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String uppercaseValue = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);

        Matcher matcher = CICLO_PATTERN.matcher(uppercaseValue);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String normalized = normalizeComparableText(value);
        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case "iii", "ciclo iii" -> "III";
            case "iv", "ciclo iv" -> "IV";
            case "v", "ciclo v" -> "V";
            case "vi", "ciclo vi" -> "VI";
            case "vii", "ciclo vii" -> "VII";
            default -> null;
        };
    }

    private String normalizeGradeName(String value) {
        String normalized = normalizeComparableText(value);
        if (normalized == null) {
            return null;
        }

        return switch (normalized) {
            case "1", "1ro", "1er", "primero", "primer" -> "1ro";
            case "2", "2do", "segundo" -> "2do";
            case "3", "3ro", "3ero", "tercero" -> "3ero";
            case "4", "4to", "cuarto" -> "4to";
            case "5", "5to", "quinto" -> "5to";
            case "6", "6to", "sexto" -> "6to";
            default -> toTitleCase(normalized);
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean sameComparableText(String left, String right) {
        String normalizedLeft = normalizeComparableText(left);
        String normalizedRight = normalizeComparableText(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeComparableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit} ]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String toTitleCase(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        String[] parts = normalized.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private record ContextoCurricularResuelto(
        Area area,
        NivelEducativo nivel,
        Ciclo ciclo,
        Grado grado
    ) {

        static ContextoCurricularResuelto empty() {
            return new ContextoCurricularResuelto(null, null, null, null);
        }

        boolean hasAnyReference() {
            return area != null || nivel != null || ciclo != null || grado != null;
        }
    }
}
