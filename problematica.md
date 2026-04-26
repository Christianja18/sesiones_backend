🧩 Problemática (versión aterrizada y clara)

En el sistema educativo peruano, los docentes deben diseñar sesiones y unidades de aprendizaje alineadas al Currículo Nacional (competencias, capacidades, desempeños, estándares, evidencias, etc.).

Sin embargo:

El proceso es manual, repetitivo y consume mucho tiempo
Requiere interpretar documentos extensos del MINEDU
Existe variabilidad en la calidad pedagógica entre docentes
Muchos docentes no dominan completamente la planificación por competencias
No hay herramientas accesibles que integren:
Currículo oficial
Contexto del aula
Generación automática de sesiones
💥 Problema central

Los docentes no cuentan con una herramienta inteligente que les permita generar sesiones y unidades de aprendizaje de manera rápida, consistente y alineada al currículo nacional peruano.

🎯 Objetivo del sistema

Desarrollar una aplicación web que:

Permita ingresar:
grado, nivel, área
competencia
contexto del aula
duración
Use IA para:
generar sesiones completas
sugerir actividades
definir evidencias e instrumentos
Asegure alineación con el currículo peruano

🚀 Siguiente nivel (recomendación de backend)

Dado que tú eres backend, esto se puede convertir en:

Dominio principal
Sesion
Unidad
Competencia
Evaluacion
Actividad
Servicios clave
SesionGeneratorService (IA)
CurriculumMapperService
EvaluationBuilderService
Integración
OpenAI / LLM
Base de datos con currículo estructurado (clave 🔥)

🧱 1. Modelo de Base de Datos (nivel profesional)

Aquí el error típico sería hacer tablas “planas”.
Tú necesitas un modelo basado en dominio educativo + normalización + flexibilidad.

🔥 Núcleo del dominio
📘 Currículo (base del sistema)
nivel_educativo (
  id,
  nombre -- Inicial, Primaria, Secundaria
)

grado (
  id,
  nivel_id,
  nombre
)

area (
  id,
  nombre -- Matemática, Comunicación, etc.
)

competencia (
  id,
  area_id,
  descripcion
)

capacidad (
  id,
  competencia_id,
  descripcion
)

desempeno (
  id,
  grado_id,
  competencia_id,
  descripcion
)
📚 Planificación
Unidad
unidad (
  id,
  titulo,
  grado_id,
  area_id,
  docente_id,
  fecha_inicio,
  fecha_fin,
  contexto,
  created_at
)
Sesión
sesion (
  id,
  unidad_id,
  titulo,
  proposito,
  duracion_minutos,
  fecha,
  generado_por_ia BOOLEAN,
  created_at
)
🧠 Contenido pedagógico
sesion_competencia (
  id,
  sesion_id,
  competencia_id
)

sesion_capacidad (
  id,
  sesion_id,
  capacidad_id
)

sesion_desempeno (
  id,
  sesion_id,
  desempeno_id
)
📝 Evaluación
criterio_evaluacion (
  id,
  sesion_id,
  descripcion
)

evidencia (
  id,
  sesion_id,
  descripcion
)

instrumento_evaluacion (
  id,
  sesion_id,
  tipo, -- rubrica, lista_cotejo
  contenido_json
)
🧩 Secuencia didáctica
actividad (
  id,
  sesion_id,
  tipo, -- inicio, desarrollo, cierre
  descripcion,
  orden
)
👤 Usuario
docente (
  id,
  nombre,
  email,
  institucion
)
🏗 2. Arquitectura Backend (Java 17 + Clean Architecture)

Aquí es donde te diferencias del resto.

📦 Estructura
domain/
  model/
  repository/
  service/

application/
  usecase/
  dto/

infrastructure/
  persistence/
  external/ (OpenAI)

presentation/
  controller/
🧠 Casos de uso clave
GenerarSesionIAUseCase
CrearUnidadUseCase
ListarCurriculoUseCase
GuardarSesionUseCase
🔥 Servicio de IA (core del negocio)
public interface AISessionGenerator {
    SesionGenerada generar(SesionInput input);
}
Implementación
@Service
public class OpenAIGeneratorService implements AISessionGenerator {

    @Override
    public SesionGenerada generar(SesionInput input) {
        String prompt = buildPrompt(input);

        // llamada a OpenAI
        String response = callLLM(prompt);

        return mapToDomain(response);
    }
}
🧩 DTO de entrada
public class SesionInput {
    String nivel;
    String grado;
    String area;
    String competencia;
    String tema;
    String contexto;
    int duracion;
}
🌐 Controller
@PostMapping("/sesiones/generar")
public ResponseEntity<SesionDTO> generar(@RequestBody SesionInput input) {
    return ResponseEntity.ok(useCase.execute(input));
}
🤖 3. Prompts optimizados por endpoint (CLAVE 🔥)

Aquí está el verdadero valor diferencial.
No uses un solo prompt gigante → divide por responsabilidad.

🎯 Prompt 1: Generar estructura base
Genera una sesión de aprendizaje basada en el Currículo Nacional del Perú.

Datos:
- Nivel: {nivel}
- Grado: {grado}
- Área: {area}
- Competencia: {competencia}
- Tema: {tema}
- Contexto: {contexto}
- Duración: {duracion}

Devuelve en JSON:
{
  "titulo": "",
  "proposito": "",
  "competencias": [],
  "capacidades": [],
  "desempenos": []
}
🎯 Prompt 2: Generar actividades
Genera la secuencia didáctica de una sesión de aprendizaje.

Incluye:
- Inicio (motivación + saberes previos)
- Desarrollo (actividades activas)
- Cierre (metacognición)

Devuelve en JSON:
{
  "inicio": [],
  "desarrollo": [],
  "cierre": []
}
🎯 Prompt 3: Evaluación
Genera evaluación formativa alineada a competencias.

Devuelve:
{
  "criterios": [],
  "evidencias": [],
  "instrumento": {
    "tipo": "rubrica",
    "detalle": []
  }
}
🎯 Prompt 4: Mejora pedagógica (opcional PRO)
Mejora esta sesión para:
- inclusión educativa
- diferenciación pedagógica
- contexto peruano

Entrada:
{sesion_json}

Salida:
versión optimizada