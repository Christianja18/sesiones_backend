# sesiones_backend

Backend Spring Boot para gestion de docentes, unidades, sesiones y documentos curriculares alineados al curriculo.

## Endpoints principales

- `POST /api/v1/docentes`
- `POST /api/v1/unidades`
- `GET /api/v1/unidades/{id}`
- `POST /api/v1/documentos-curriculo` (`application/json`, campos `tipo`, `nombreArchivo`, `archivoUrl`)
- `POST /api/v1/documentos-curriculo/{id}/procesar`
- `GET /api/v1/documentos-curriculo/{id}`
- `GET /api/v1/curriculum/contexto`
- `POST /api/v1/sesiones/generar`
- `POST /api/v1/sesiones`
- `GET /api/v1/sesiones/{id}`

## Documentacion

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Notas

- La generacion de sesiones quedo abstraida en un servicio reemplazable.
- En esta version se entrega un generador deterministico de plantilla, listo para sustituirse por un adaptador LLM cuando exista contrato y credenciales.
- `documento_curriculo` registra la URL del PDF y su tipo: `curriculo` para competencias/capacidades, `programa` para desempenos por grado.
