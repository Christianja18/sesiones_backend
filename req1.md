# 🧠 Requerimiento: Ingesta Completa del Programa Curricular con IA

## Sistema de Planificación Académica basado en el Currículo Peruano

---

# 📚 Contexto Pedagógico

El sistema debe ingerir información del **Programa Curricular de Educación Primaria**, el cual:

* Organiza el aprendizaje por:

  * áreas
  * competencias
  * capacidades
  * estándares (por ciclo)
  * desempeños (por grado)
* Los desempeños están alineados con competencias y capacidades ([SITEAL][1])

---

# ⚠️ Problema Actual

El modelo actual:

✔ almacena competencias, capacidades y desempeños
❌ NO almacena relaciones pedagógicas completas

Esto provoca:

* pérdida de trazabilidad curricular
* generación IA menos precisa
* dificultad para validar sesiones

---

# 🎯 Objetivo del Requerimiento

Permitir la **ingesta completa y estructurada del currículo**, incluyendo:

* estándares por ciclo
* relación desempeño ↔ capacidad
* progresión curricular
* trazabilidad pedagógica

---

# 🧱 Nuevas Tablas Requeridas

---

## 📊 1. estandar_aprendizaje (OBLIGATORIA)

```sql
CREATE TABLE estandar_aprendizaje (
    id INT AUTO_INCREMENT PRIMARY KEY,
    competencia_id INT NOT NULL,
    ciclo_id VARCHAR(10) NOT NULL,
    descripcion TEXT NOT NULL,
    FOREIGN KEY (competencia_id) REFERENCES competencia(id) ON DELETE CASCADE,
    FOREIGN KEY (ciclo_id) REFERENCES ciclo(id) ON DELETE CASCADE,
    UNIQUE KEY uk_estandar (competencia_id, ciclo_id, descripcion(255))
);
```

---

## 📊 2. desempeno_capacidad (OBLIGATORIA)

```sql
CREATE TABLE desempeno_capacidad (
    desempeno_id INT NOT NULL,
    capacidad_id INT NOT NULL,
    PRIMARY KEY (desempeno_id, capacidad_id),
    FOREIGN KEY (desempeno_id) REFERENCES desempeno(id) ON DELETE CASCADE,
    FOREIGN KEY (capacidad_id) REFERENCES capacidad(id) ON DELETE CASCADE
);
```

---

## 📊 3. estandar_desempeno (RECOMENDADA)

```sql
CREATE TABLE estandar_desempeno (
    estandar_id INT NOT NULL,
    desempeno_id INT NOT NULL,
    PRIMARY KEY (estandar_id, desempeno_id),
    FOREIGN KEY (estandar_id) REFERENCES estandar_aprendizaje(id) ON DELETE CASCADE,
    FOREIGN KEY (desempeno_id) REFERENCES desempeno(id) ON DELETE CASCADE
);
```

---

## 📊 4. ingest_log (DEBUG Y CONTROL IA)

```sql
CREATE TABLE ingest_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento_id INT,
    chunk_id INT,
    estado ENUM('OK','ERROR'),
    mensaje TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (documento_id) REFERENCES documento_curriculo(id),
    FOREIGN KEY (chunk_id) REFERENCES documento_chunk(id)
);
```

---

# ⚙️ Modificaciones a Tablas Existentes

---

## 📊 Tabla: desempeno

```sql
ALTER TABLE desempeno
ADD fuente ENUM('oficial','ia') DEFAULT 'oficial',
ADD ciclo_id VARCHAR(10) NULL,
ADD FOREIGN KEY (ciclo_id) REFERENCES ciclo(id);
```

---

## 📊 Tabla: actividad (mejora de escalabilidad)

```sql
CREATE TABLE tipo_actividad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE
);
```

---

```sql
ALTER TABLE actividad
ADD tipo_id INT,
ADD FOREIGN KEY (tipo_id) REFERENCES tipo_actividad(id);
```

---

# 🤖 Pipeline de Ingesta con IA (OBLIGATORIO)

---

## Flujo

```text
1. Registrar documento_curriculo (tipo = 'programa')

2. Backend:
   - descarga PDF
   - extrae texto

3. División:
   - dividir en chunks semánticos

4. IA:
   - analiza cada chunk
   - extrae:
        area
        competencia
        capacidades
        ciclo
        grado
        desempenos
        estandares

5. Backend:
   - validar contra BD
   - evitar duplicados

6. Persistencia:
   - competencia
   - capacidad
   - estandar_aprendizaje
   - desempeno
   - desempeno_capacidad
   - estandar_desempeno

7. Logging:
   - registrar en ingest_log

8. Marcar documento como PROCESADO
```

---

# 🧠 Prompt para IA

```text
Actúa como especialista en el currículo educativo peruano.

Analiza el contenido del Programa Curricular y extrae:

- área
- competencia
- capacidades
- estándares (por ciclo)
- desempeños (por grado)
- nivel
- ciclo
- grado

Devuelve SOLO JSON:

{
  "area": "",
  "competencia": "",
  "capacidades": [],
  "estandares": [],
  "desempenos": [],
  "nivel": "",
  "grado": "",
  "ciclo": ""
}

No inventes información.
```

---

# ⚠️ Reglas Críticas

### ❌ NO HACER

* No usar solo el Currículo Nacional para desempeños
* No insertar sin validar duplicados
* No ignorar relaciones pedagógicas

---

### ✅ HACER

* Procesar por chunks
* Validar área, grado y ciclo
* Relacionar desempeño con capacidad
* Relacionar desempeño con estándar

---

# 🧠 Modelo Final Esperado

```text
Nivel
 → Ciclo
   → Grado
     → Área
       → Competencia
         → Capacidad
         → Estándar (por ciclo)
         → Desempeño (por grado)
             ↔ Capacidad
             ↔ Estándar
```

---

# 🚀 Resultado Esperado

El sistema será capaz de:

* ingerir completamente el programa curricular
* almacenar relaciones pedagógicas reales
* generar sesiones coherentes con el MINEDU
* escalar como plataforma educativa

---

# 💡 Insight Final

> ❗ No basta con guardar datos
> 👉 debes guardar la estructura pedagógica completa

---

[1]: https://siteal.iiep.unesco.org/bdnp/500/programa-curricular-educacion-primaria?utm_source=chatgpt.com "Programa Curricular de Educación Primaria | SITEAL"
