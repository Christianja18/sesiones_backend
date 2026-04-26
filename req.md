# 🧠 Pipeline de Ingesta Curricular con IA (Corrección de Desempeños)

## Sistema basado en el Currículo Nacional de la Educación Básica

---

## 📚 Contexto real del currículo

El sistema utiliza como base el **Currículo Nacional de la Educación Básica** y los **Programas Curriculares de Educación Básica Regular**.

👉 Importante:

* El Currículo Nacional define:

  * competencias
  * capacidades
  * estándares por ciclo

* Los Programas Curriculares contienen:

  * desempeños por grado ✔

📌 Confirmación:
Los programas curriculares **sí incluyen desempeños por grado alineados a competencias y capacidades** ([Gobierno del Perú][1])

---

# ⚠️ PROBLEMA DETECTADO

La IA no está llenando la tabla `desempeno` porque:

```text
Se está usando solo el PDF del Currículo Nacional
```

👉 Pero ese documento:

* ❌ No contiene desempeños detallados por grado
* ❌ Solo tiene estándares por ciclo

---

# 🧠 CAUSA RAÍZ

```text
1 fuente (Currículo Nacional) ≠ modelo completo de BD
```

👉 Estás intentando extraer:

* competencia ✔
* capacidad ✔
* desempeño ❌ (no existe ahí como tal)

---

# ✅ SOLUCIÓN ARQUITECTÓNICA

## 🔥 Separar fuentes de datos

```text
Fuente 1 → Currículo Nacional
  → competencia
  → capacidad

Fuente 2 → Programas Curriculares
  → desempeño
```

---

# 🧱 ACTUALIZACIÓN DE TABLA

## ➕ documento_curriculo

```sql
ALTER TABLE documento_curriculo
ADD tipo ENUM('curriculo', 'programa') NOT NULL;
```

---

# 🤖 LÓGICA DE INGESTA

```text
if documento.tipo == 'curriculo':
    extraer:
        - area
        - competencia
        - capacidades

if documento.tipo == 'programa':
    extraer:
        - area
        - grado
        - ciclo
        - desempenos
```

---

# 🔄 PIPELINE CORREGIDO

```text
1. Registrar documento (PDF)

2. Backend:
   - descarga PDF
   - extrae texto

3. Clasificación:
   - identificar tipo (curriculo o programa)

4. IA:
   - analiza chunks

5. Backend:
   if curriculo:
       guardar competencia + capacidad

   if programa:
       guardar desempeno

6. Relación:
   - vincular desempeño con competencia + grado

7. Marcar como procesado
```

---

# 🧠 PROMPT AJUSTADO PARA IA

```text
Actúa como especialista en el currículo educativo peruano.

Si el contenido pertenece a un Programa Curricular:
- extrae desempeños por grado

Si pertenece al Currículo Nacional:
- extrae competencias y capacidades

Devuelve JSON:

{
  "tipo": "curriculo | programa",
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

### ❌ NO HACER

* No esperar desempeños del Currículo Nacional
* No usar una sola fuente para todo
* No forzar a la IA a inventar desempeños

---

### ✅ HACER

* Usar Programas Curriculares para desempeños
* Separar lógica por tipo de documento
* Validar antes de insertar en BD

---

# 🧠 OPCIÓN AVANZADA (FALLBACK IA)

Si no hay programas curriculares disponibles:

👉 permitir generación IA controlada

---

## ➕ mejora en tabla desempeño

```sql
ALTER TABLE desempeno
ADD fuente ENUM('oficial','ia') DEFAULT 'oficial';
```

---

## Lógica:

```text
if no hay desempenos:
    generar con IA
    guardar como fuente = 'ia'
```

---

# 🚀 RESULTADO FINAL

El sistema ahora:

* ✔ obtiene competencias reales
* ✔ obtiene capacidades reales
* ✔ obtiene desempeños reales (desde programas)
* ✔ evita datos incorrectos

---

# 💡 INSIGHT CLAVE

> ❗ El Currículo Nacional define el marco
> ❗ Los Programas Curriculares lo operativizan

👉 Necesitas ambos para que tu sistema funcione correctamente

---

# 🏁 CONCLUSIÓN

Si no usas programas curriculares:

```text
tu tabla desempeño siempre estará vacía
```

Si los integras correctamente:

```text
tendrás un sistema completo y alineado al MINEDU
```

---

[1]: https://www.gob.pe/90158-ministerio-de-educacion-programas-curriculares-de-la-educacion-basica-regular?utm_source=chatgpt.com "Programas curriculares de la Educación Básica Regular - Contenido institucional - Ministerio de Educación - Plataforma del Estado Peruano"
