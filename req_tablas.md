# 🧠 Pipeline de Ingesta Curricular con IA

## Sistema basado en el Currículo Nacional de la Educación Básica

---

## 📚 Contexto

El sistema se basa en el **Currículo Nacional de la Educación Básica**, el cual:

* Está estructurado por **competencias, capacidades y desempeños** ([Maestro Excelencia][1])
* Se organiza por **niveles, ciclos y grados** ([Maestro Excelencia][1])
* Define aprendizajes esperados progresivamente por ciclo ([Ministerio de Educación][2])

👉 Esto implica que **NO está diseñado como base de datos**, sino como documento pedagógico.

---

# ⚠️ PROBLEMA A RESOLVER

El sistema necesita:

> Leer un PDF del currículo → extraer información → almacenarla estructurada

Pero:

* ❌ El PDF mezcla múltiples áreas
* ❌ No está segmentado por grado limpio
* ❌ Usa ciclos como unidad principal

👉 Conclusión:

> ❗ `area_id` y `grado_id` NO pueden asignarse al inicio

---

# 🧱 DISEÑO DE TABLA DOCUMENTO

```sql
CREATE TABLE documento_curriculo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_archivo VARCHAR(255),
    archivo_url VARCHAR(500) NOT NULL,
    procesado BOOLEAN DEFAULT FALSE,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

👉 🔥 Regla:

* NO guardar `area_id` ni `grado_id` aquí
* El documento es **fuente bruta**, no clasificada

---

# 🧩 UNIDAD REAL DE PROCESAMIENTO: CHUNKS

```sql
CREATE TABLE documento_chunk (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento_id INT,
    contenido TEXT,
    procesado BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (documento_id) REFERENCES documento_curriculo(id)
);
```

👉 Cada chunk = fragmento del PDF

---

# 🤖 CLASIFICACIÓN CON IA

```sql
CREATE TABLE documento_clasificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento_id INT,
    area_id INT,
    grado_id INT,
    ciclo_id VARCHAR(10),
    confianza DECIMAL(5,2),
    FOREIGN KEY (documento_id) REFERENCES documento_curriculo(id)
);
```

👉 Permite:

* múltiples áreas por documento ✔
* múltiples grados ✔
* clasificación progresiva ✔

---

# 🔄 PIPELINE COMPLETO

```text
1. Registrar documento (URL PDF)

2. Backend:
   - descarga PDF
   - extrae texto (parser)

3. División:
   - dividir en chunks semánticos

4. IA:
   - analizar cada chunk
   - extraer estructura curricular

5. Backend:
   - validar datos
   - mapear a BD

6. Persistencia:
   - competencia
   - capacidad
   - desempeño

7. Clasificación:
   - guardar área + grado + ciclo

8. Marcar documento como procesado
```

---

# 🧠 PROMPT PARA IA

```text
Actúa como especialista en el Currículo Nacional del Perú.

Analiza el siguiente contenido y extrae:

- área
- competencia
- capacidades
- desempeños
- nivel
- grado (si aplica)
- ciclo

Devuelve SOLO JSON:

{
  "area": "",
  "competencia": "",
  "capacidades": [],
  "desempenos": [],
  "nivel": "",
  "grado": "",
  "ciclo": ""
}

No inventes información.
```

---

# ⚠️ REGLAS CRÍTICAS

### ❌ NO hacer

* No enviar el PDF completo a la IA
* No asignar grado directo al documento
* No confiar en texto sin validar

---

### ✅ HACER

* Procesar por chunks
* Validar contra BD (área, grado, ciclo)
* Evitar duplicados
* Normalizar texto

---

# 🧠 MAPEO A BASE DE DATOS

Ejemplo:

```json
{
  "area": "Comunicación",
  "grado": "3°",
  "ciclo": "VII"
}
```

---

## Resolución backend:

```sql
SELECT id FROM area WHERE nombre = 'Comunicación';

SELECT g.id
FROM grado g
JOIN ciclo c ON g.ciclo_id = c.id
WHERE g.nombre = '3°' AND c.id = 'VII';
```

---

# 🚀 RESULTADO FINAL

El sistema logra:

* 📄 Ingesta automática del currículo
* 🧠 Transformación con IA
* 🧱 Estructuración en BD
* 🔄 Reutilización para generación de sesiones

---

# 💡 INSIGHT CLAVE

> ❗ El PDF NO es la unidad de conocimiento
> 👉 el **chunk procesado + validado sí lo es**

---

# 🏁 CONCLUSIÓN

Este diseño permite:

* escalar a múltiples documentos
* integrar IA de forma controlada
* evitar errores de modelado curricular
* construir un sistema SaaS educativo robusto

---

[1]: https://maestroexcelencia.pe/que-es-el-curriculo-nacional/?utm_source=chatgpt.com "¿Qué es el currículo nacional? - Maestro Excelencia"
[2]: https://www.minedu.gob.pe/curriculo/documentos.php?utm_source=chatgpt.com "Currículo Nacional | Minedu"
