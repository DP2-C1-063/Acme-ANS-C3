# Plan de Pruebas — Realm: Technician

Fecha: 2025-10-14

Este documento describe un plan de pruebas detallado para el realm Technician. Cada prueba incluye: feature, objetivo, precondiciones, pasos exactos a ejecutar, datos a usar (atributos), y verificaciones/asserciones esperadas. El objetivo es que puedas rehacer todos los tests (.safe / .hack) a partir de estas instrucciones.

## Índice
- Alcance y prioridades
- Estructura de cada test
- Mantenimiento (MaintenanceRecord)
- Tareas asociadas (TaskInvolvesRecord)
- Dashboard del técnico
- Pruebas de seguridad generales
- Prioridad y orden sugerido de ejecución
- Notas finales

## Alcance y prioridades

Prioridad alta (empezar por estas):
- Autorización/roles (acceso sólo al propietario)
- Regla de negocio: publicar sólo si hay tareas
- Validaciones de fechas (nextInspection > maintenanceMoment)
- Inyecciones y XSS

Prioridad media:
- Validaciones de campos monetarios (amount, currency)
- Integridad en la asociación MaintenanceRecord ↔ Task
- Cálculos del dashboard (promedios, desviaciones)

Prioridad baja:
- Límites de longitud de texto
- Paginación y ordenación de listados

## Estructura de cada test (plantilla)

Cada test debe documentarse y ejecutarse siguiendo este esquema:

1. Feature: breve nombre de la funcionalidad bajo prueba.
2. Objetivo: qué valida el test (una frase).
3. Precondiciones: datos que deben existir (usuario, IDs, estados de entidades).
4. Pasos (step-by-step): acciones concretas (GET/POST, rutas, parámetros). Cada paso debe enumerarse para reproducibilidad.
5. Datos de entrada: lista de atributos y valores que se envían.
6. Verificaciones (assertions): lista de comprobaciones que confirman el resultado esperado.
7. Resultado esperado: éxito/fracaso y mensajes.
8. Limpieza (opcional): revertir cambios si el test modifica datos importantes.

Usa este esquema para crear los archivos .hack (maliciosos/negativos) y .safe (positivos/validación) correspondientes.

---

## Mantenimiento — MaintenanceRecord

Para la entidad MaintenanceRecord prueba todas las operaciones: list, show, create, update, publish. A continuación los tests recomendados y los detalles necesarios para replicarlos.

Nota sobre atributos (contrato mínimo):
- maintenanceMoment (DateTime) — se asigna automáticamente al crear; verificar formato y que no pueda enviarse arbitrariamente.
- status (ENUM: PENDING, IN_PROGRESS, COMPLETED) — requerido.
- nextInspection (DateTime) — debe ser posterior o igual a maintenanceMoment según reglas de negocio.
- estimatedCost (Money: amount:Number, currency:String) — amount >= 0, max 2 decimales; currency ∈ {EUR, USD, GBP}.
- notes (String) — campo libre; validar length, escapes y sanitización.
- relatedAircraft (ID) — requerido; debe existir y pertenecer al catálogo.
- draftMode (boolean) — true por defecto al crear; sólo el sistema puede cambiarlo al publicar.
- technician (referencia al propietario) — asumir el usuario autenticado; no debe permitirse sobrescribir.

### Test: List — positivo
Feature: Listar maintenance records del técnico autenticado.
Objetivo: comprobar visibilidad sólo de los records del técnico y presencia de columnas.
Precondiciones: usuario `technicianX` con >=1 record (mix draft/published).
Pasos:
1. Login como `technicianX`.
2. GET `/technician/maintenance-record/list`.
Verificaciones:
- Status 200 y vista renderizada.
- Tabla con columnas: Maintenance Moment, Status, Estimated Cost, Related Aircraft.
- Cada fila tiene enlace a /show y corresponde a `technicianX`.
Edge cases:
- Paginación con >50 registros; ordenar por maintenanceMoment.

### Test: List — negativo (sin auth / rol equivocado)
Objetivo: asegurar acceso restringido.
Precondiciones: ninguna sesión.
Pasos:
1. GET `/technician/maintenance-record/list` sin cookie de sesión.
Verificaciones: redirección a login o error 403/401.
También probar con usuario `administrator` y comprobar acceso denegado.

### Test: Show — positivo (borrador)
Feature: Mostrar record propio en draft.
Precondiciones: record `R1` con draftMode=true y perteneciente a `technicianX`.
Pasos:
1. Login `technicianX`.
2. GET `/technician/maintenance-record/show?id=R1`.
Verificaciones:
- Campos visibles y con formato correcto: maintenanceMoment (timestamp), status (enum), nextInspection (date), estimatedCost ("123.45 EUR"), notes (texto), relatedAircraft (nombre).
- draftMode = Yes y botones Update/Publish visibles.
- Enlace a Tasks: `/technician/task-involves-record/list?masterId=R1`.

### Test: Show — negativo (otro técnico / id inexistente)
Precondiciones: `R2` pertenece a otro técnico.
Pasos:
1. Login `technicianX`.
2. GET `/technician/maintenance-record/show?id=R2`.
Verificaciones: acceso denegado (panic/403).
También probar id inexistente => comprobar 404 o error gestionado.

### Test: Create — positivo (campos válidos)
Feature: Crear maintenance record.
Objetivo: asegurarse de que la creación funciona, auto-assign de maintenanceMoment y valores por defecto.
Precondiciones: usuario técnico con al menos una aeronave válida `A1`.
Pasos:
1. Login `technicianX`.
2. GET `/technician/maintenance-record/create` (comprobar dropdown de aircrafts).
3. POST `/technician/maintenance-record/create` con payload:
   - status = PENDING
   - nextInspection = YYYY-MM-DD HH:mm (futuro razonable)
   - estimatedCost.amount = 1500.50
   - estimatedCost.currency = EUR
   - notes = "Routine check"
   - relatedAircraft = A1
Verificaciones:
- Redirect a list o show con mensaje de éxito.
- Nuevo record presente en DB con draftMode = true.
- maintenanceMoment asignado por el backend y razonable (ahora ±5s).

### Test: Create — validaciones negativas (campo por campo)
Realizar una batería de tests donde se deja cada atributo inválido uno a uno y verificar la validación exacta:

- status = NULL -> error required (NotNull).
- status = "BAD" -> error enum/invalid value.
- nextInspection < maintenanceMoment -> error de regla de negocio (mensaje: acme.validation.maintenance-record.invalid-dates.message).
- nextInspection = NULL -> si es requerido, mostrar error.
- estimatedCost.amount negativo -> error PositiveOrZero.
- estimatedCost.amount con >2 decimales -> error de formato o redondeo.
- estimatedCost.currency = "JPY" -> error moneda no permitida.
- relatedAircraft vacío o id inexistente -> error NotNull o Panic.
- notes muy largo (por encima del límite) -> error de longitud.

Para cada caso documentar el payload, el mensaje de error esperado y si el formulario debe mantenerse en pantalla.

### Test: Create — seguridad de entrada

- SQL Injection: notes = "'; DROP TABLE maintenance_record; --". Verificaciones: el texto se guarda como literal y la BD no se altera.
- XSS: notes = "<script>alert(1)</script>". Verificaciones: al mostrar, el script no se ejecuta y el HTML aparece escapado.

### Test: Update — positivo (draft)
Feature: Actualizar record del técnico en draft.
Precondiciones: record R1 draftMode=true.
Pasos:
1. Login técnico.
2. GET `/technician/maintenance-record/update?id=R1` y comprobar que el formulario trae los valores actuales.
3. POST con cambios válidos en status, nextInspection, estimatedCost, notes, relatedAircraft.
Verificaciones:
- Redirect a show con mensaje de éxito.
- Los cambios persistidos reflejados en la vista show.

### Test: Update — negativos

- Intentar update sobre record publicado (draftMode=false) -> acceso denegado.
- Intentar update de record de otro técnico -> acceso denegado.
- Probar validaciones idénticas a Create (nextInspection inválida, coste negativo, etc.).

### Test: Publish — positivo
Feature: Publicar record en draft que cumpla condiciones.
Precondiciones: record Rpublish con draftMode=true y al menos 1 tarea asociada publicada según regla de negocio.
Pasos:
1. Login técnico propietario.
2. GET `/technician/maintenance-record/publish?id=Rpublish`.
3. POST confirmando publicación.
Verificaciones:
- draftMode cambia a false.
- Redirect a show y botones Update/Publish desaparecen.
- Mensaje de éxito presente.

### Test: Publish — negativos (reglas de negocio)

- Sin tareas -> error acme.validation.maintenance-record.no-task.message.
- Con tareas en draft -> error acme.validation.maintenance-record.published-task.message.
- Ya publicado -> acceso denegado/invalid operation.

---

## Tareas asociadas — TaskInvolvesRecord

Aplica las mismas categorías: list, show, create, update, publish (si aplica). Tests clave:

### Test: List tasks de un record
Feature: Listar tasks asociadas a un maintenance record (masterId).
Precondiciones: record R con varias tareas.
Pasos:
1. Login técnico propietario.
2. GET `/technician/task-involves-record/list?masterId=R`.
Verificaciones:
- Tabla con columnas: Technician, Type, Description, Priority, Estimated duration.
- Botón "Add task" sólo si R.draftMode = true.
- Sólo aparecen tareas de R.

### Test: Show task
Feature: Mostrar detalle de una asociación task↔record.
Verificaciones:
- Campos: task id/nombre, record's aircraft, type, description, priority, estimated duration, technician.
- Acceso denegado si el record no pertenece al usuario.

### Test: Create task-involves-record — positivo
Feature: Asociar tarea existente al record en draft.
Precondiciones: record R draftMode=true, task T disponible.
Pasos:
1. GET `/technician/task-involves-record/create?masterId=R`.
2. POST con form.masterId=R, form.task=T.
Verificaciones:
- Redirect a list y la nueva asociación aparece.

### Test: Create — negativos

- masterId inexistente -> panic.
- masterId de otro técnico -> acceso denegado.
- record publicado -> no permitido.
- task NULL/inexistente -> validación.
- intentos de inyección en campos de texto (description) -> sanitización.

### Test: Update/Publish task-involves-record

- Update sólo si el record está en draft y pertenece al técnico.
- Probar cambios de task id y comprobar integridad referencial.

---

## Dashboard (TechnicianDashboard)

Objetivo: comprobar que el dashboard del técnico muestra los indicadores correctos y que los cálculos son correctos sobre los datos publicados pertinentes.

Campos clave a validar:
- numberMaintenanceRecordPending / InProgress / Completed (enteros >= 0, cuentan sólo records del técnico y publicados según caso requerido).
- recordWithNearestInspection (debe devolver el record publicado con la próxima fecha de inspección más cercana).
- top5AircraftsWithMostTasks (lista hasta 5 aircrafts ordenada por número de tasks).
- average/minimum/maximum/standardDeviationEstimatedCostLastYear (Money) — comprobar cálculo con datos de test insertados en el rango de fechas del último año.
- average/minimum/maximum/standardDeviationEstimatedDurationTask (num) — verificar cálculo.

Tests recomendados:

1. Dashboard con datos completos: crear un set de datos controlado (n records con costes y duraciones conocidos) y verificar los cálculos numéricos exactos.
2. Dashboard sin records publicados: todos los contadores deben ser 0 o N/A.
3. Acceso no autorizado: rol distinto o sin login -> acceso denegado.

---

## Pruebas de seguridad (generales)

Lista de ataques/verificaciones que deben incluirse como tests `.hack`:

1. SQL Injection: probar en todos los campos de texto (notes, descriptions) y en parámetros URL. Esperado: no ejecución y escapado/intactitud de la BD.
2. XSS: inyectar HTML/JS y validar que la salida se escapa y no ejecuta.
3. CSRF: enviar POST sin token válido y comprobar 403/denegación.
4. Path Traversal: inyectar rutas (../../) en parámetros y verificar que no accede al filesystem ni expone archivos.
5. Manipulación de IDs y parámetros: cambiar masterId, recordId, technicianId en formularios/requests y comprobar que la autorización del servidor impide cambios no permitidos.
6. Session fixation/hijacking: comprobar que al iniciar sesión en otro navegador las sesiones son distintas y que no se puede usar una sesión válida para acciones de otro usuario.

Para cada test de seguridad documentar:
- payload exacto
- ruta utilizada
- precondiciones
- verificación de que no hay impacto en la BD ni ejecución de código

---

## Ejecución: orden sugerido de pruebas

1. Autorización básica: listar, show (usuarios correctos/incorrectos).
2. Crear y validar creación positiva.
3. Validaciones de creación (campo por campo negativos).
4. Asociar tareas y comprobar reglas de negocio (no publicar sin tareas).
5. Actualizar y validar actualización sobre borrador; bloquear publicaciones/updates sobre publicados.
6. Publicación: probar positivo y los negativos (sin tareas, tareas en draft, ya publicado).
7. Dashboard: llenar datos controlados y validar cálculos.
8. Pruebas de seguridad (SQLi, XSS, CSRF, path traversal) — ejecutar tanto sobre endpoints de creación/actualización como sobre parámetros de lista/show.

## Plantillas y outputs esperados

Para cada test crea dos ficheros si aplica:
- `xxxx-positive.safe` — caso positivo (flujo feliz).
- `xxxx-negative.hack` — caso negativo o de inyección/seguridad.

En cada fichero de test incluir al inicio (metadatos):
- sign-in (username/password)
- driver/ruta (GET/POST)
- parámetros/form fields con nombres exactos usados por la app
- assertions esperadas (status, redirect, error message keys)

Ejemplo de payload (Create positivo):

```text
sign-in.username = technician1
sign-in.password = technician1
driver.post = /technician/maintenance-record/create
form.status = PENDING
form.nextInspection = 2026-01-15 10:00
form.estimatedCost.amount = 1500.50
form.estimatedCost.currency = EUR
form.notes = Routine check
form.relatedAircraft = 42
assert.redirect = /technician/maintenance-record/list
assert.message = acme.messages.technician.maintenanceRecord.form.success.create
```

## Notas sobre datos y limpieza

- Usa usuarios de prueba dedicados (ej.: `technician1`, `technician2`) y aeronaves/tasks creadas específicamente para testing.
- Para tests que alteran estado (create/update/publish) preparar scripts de limpieza o fixtures que permitan restaurar un estado conocido antes de cada suite.
- Para pruebas numéricas del dashboard, crea datos deterministas para poder comprobar cálculos exactos.

## Entrega y seguimiento

- Genera los ficheros `.safe` y `.hack` basados en los tests documentados en este plan.
- Adjunta un pequeño README con cómo ejecutar las pruebas en local (rutas, usuarios de prueba, fixtures necesarios).
- Prioriza las pruebas de seguridad y autorización al principio.

---

## Resumen final

He incluido instrucciones paso a paso para cada tipo de test que debes rehacer para el realm Technician: qué probar en cada campo/atributo, cómo manipular los datos, qué mensajes de error esperar, y una orden sugerida de ejecución. Si quieres, puedo:

1. Generar automáticamente los ficheros `.hack`/`.safe` esqueleto en `src/test/resources/tests/technician/` usando las plantillas mostradas.
2. Preparar un conjunto de fixtures SQL/JSON para poblar dados de prueba.

Indícame si prefieres que genere los ficheros de pruebas (y cuántos de los listados quieres primero).
