## Especificación del Proyecto (Requirements)

#### Problema que se quiere resolver:

La institución no tiene un sistema informático unificado para almacenar y modificar información de las personas que la conforman, tal como datos personales, contacto, condición o cargo, materias asociadas; ni para llevar registro de actividades, tal como inscripciones a materias y exámenes finales (y sus requisitos), y demás tareas administrativas.



#### Usuarios del sistema:

* **Personal de la Oficina de Alumnos**, como administradores (ABM).
* **Estudiantes:** consultar su información, inscribirse en materias.
* **Profesores:** cargar notas, consultar listados de sus alumnos.



#### Funcionalidades principales :

* **Gestionar estudiante** (Alta, Baja, Modificación, Consulta y Listado de estudiante).
* **Gestionar profesor** (Alta, Baja, Modificación, Consulta y listado de profesor).
* **Gestionar carrera** (Alta, Baja, Modificación, Consulta y listado de carrera).
* **Gestionar materia** (Alta, Baja, Modificación, Consulta y listado de materia).
* **Gestionar plan de estudio** (Alta, Baja, Modificación, consulta y Listado de plan).
* **Gestionar inscripción a materia** (Alta y Baja de Inscripción, y validación automática de correlatividades, Consulta ).
* **Gestionar exámenes finales** (Alta de exámenes finales, Carga de notas).
* **Inscripción a exámenes finales** (Alta y Baja de Inscripción, validación automática de correlatividades y de condición del alumno, generación de acta de examen).
* **Consultar materias de alumno** (materias cursando, materias aprobadas, materias desaprobadas).
* **Gestionar asignar profesores a materias** durante cierto período académico, y su respectivo rol.
* **Envío de notificaciones:** los administradores podrían enviar notificaciones a alumnos en particular, y los profesores podrían enviar notificaciones tipo “broadcast”, dirigidas a todos los alumnos de cierta materia.
* **Herramienta para hacer seguimiento de los estudiantes:** “ver su progreso, detectar si están en riesgo de abandonar o, por el contrario, si van muy bien y podríamos ofrecerles algún programa especial.”





#### Restricciones técnicas:

* Disponibilidad
* Interfaz amigable
* Bajo nivel de falla
* Seguridad de datos personales (información delicada)
* Escalabilidad (para soportar el crecimiento de la institución y, por lo tanto, del

sistema)

* Facilidad de mantenimiento (por lo de la herramienta que quieren agregar)
* Portable (sugerencia, para poder acceder cómodamente con distintos dispositivos)





#### Tamaño del equipo:

4 personas.



#### Tecnologías elegidas y justificación:

La web permite portabilidad y acceso desde cualquier dispositivo, por eso utilizamos un stack tecnológico web.

Ya teníamos un conocimiento de Java, por eso utilizamos el framework Spark para poder utilizar los protocolos HTTP.

* **Spark:** Ofrece un manejo amigable de solicitudes HTTP.
* **ActiveJDBC:** Abstrae la configuración y el uso de la base de datos.
* **Mustache:** este motor de plantillas permite la separación de responsabilidades, ya que no escribimos la lógica de negocio dentro del html.
* **Tailwind:** Permite construir interfaces modernas y complejas utilizando clases preestablecidas, como la posibilidad de implementar un modo oscuro.
* **SQLite:** Prácticamente no requiere configuración y se basa en un manejo sin servidor (server-less) de la base de datos, además de que ofrece mucha más portabilidad y sencillez en comparación a otros motores.
* **BCrypt:** para seguridad y encriptado de las contraseñas
* **Protocolo SMTP:** Utilizado para las funcionalidades que requieren enviar e-mails a los usuarios.



#### Plazo estimado:

Hasta junio de 2026.



#### Cambios de alcance ocurridos:

* El nombre de usuario pasó a ser el DNI de la persona para facilitar la identificación y diferenciación de los mismos como Student/Teacher.
* Modificación de estados en Planes de Estudio y Carreras: Se determinó que no se realizará el borrado físico (DELETE) de los mencionados, sino que solo se modificará su estado. Esto previene impactos negativos en la integridad del historial académico, evitando que los campos de planes de estudiantes graduados queden en null. Lo mismo con los planes de estudio en el caso de que se pudiera eliminar una carrera.



#### Problemas encontrados:

* Curva de aprendizaje alta para empezar a manejar parcialmente todas las herramientas necesarias.
* Dificultades con git, lo que a su vez complicó el reparto de tareas en ciertas situaciones.
* Es nuestro primer proyecto grupal en la carrera, por lo que estamos en un proceso continuo de aprendizaje y prueba-error.
* Empezamos a notar que el sistema tenía un alto nivel de acoplamiento, por lo que tuvimos que refactorizar.



#### Forma de organización del equipo:

Hacemos reuniones virtuales o presenciales de organización y división de tareas, luego cada uno implementa la tarea que le tocó, y si surgen dudas, nos comunicamos.


<br>

## (Auditoría) Análisis de Riesgos

### a) Riesgos identificados por la IA (LLM)
> **Nota:** Los riesgos extraídos del análisis de Gemini fueron clasificados según la estructura de auditoría solicitada.

| Identificado | Tipo de Riesgo | Descripción | Probabilidad | Impacto |
| :--- | :--- | :--- | :--- | :--- |
| **R1** | Técnico | Bloqueos en SQLite por concurrencia de datos durante picos de uso (ej. inscripciones). | Alta | Crítico |
| **R2** | Técnico | Fallos lógicos complejos en el motor de validación de correlatividades (regresión de código). | Media | Alto |
| **R3** | Técnico | Fugas de recursos y caída del sistema por mal cierre de transacciones en Spark/ActiveJDBC. | Baja | Crítico |
| **R4** | Planificación | Desvío del alcance por subestimar la complejidad de la herramienta de seguimiento analítico. | Alta | Alto |
| **R5** | Organizacional | Conflictos y cuellos de botella por alto acoplamiento y dependencias entre tareas del equipo. | Media | Alto |
| **R6** | Técnico | Degradación de la UI por latencia al enviar notificaciones SMTP de forma sincrónica. | Media | Medio |
| **R7** | Organizacional | Resistencia al cambio del personal administrativo hacia el nuevo sistema integral. | Media | Alto |

### b) Riesgos identificados por el Equipo
> **Nota:** Clasificados e interpretados a partir del borrador original del equipo.

| Identificado | Tipo de Riesgo | Descripción | Probabilidad | Impacto |
| :--- | :--- | :--- | :--- | :--- |
| **E1** | Planificación | Curva alta de aprendizaje tecnológico que retrasa las entregas. | Alta | Alto |
| **E2** | Humano | Abandono del proyecto por parte de uno o más integrantes del grupo. | Baja | Crítico |
| **E3** | Técnico | Problemas de integración de versiones y pérdida de código por inexperiencia con GitHub. | Alta | Medio |
| **E4** | Organizacional | Desconocimiento profundo de los procesos y reglas de negocio reales de una universidad. | Media | Alto |
| **E5** | Técnico | Incompatibilidad del sistema con los equipos de hardware o servidores finales de la institución. | Baja | Medio |
| **E6** | Organizacional | Cambios repentinos en las normativas del estado o de la universidad que requieran reestructuración. | Baja | Alto |
| **E7** | Organizacional | Factores externos extremos (quiebra de la institución, paros docentes/administrativos). | Baja | Alto |

### c) Comparación de Análisis

**Riesgos que encontró la IA y el equipo no:**
* **Limitaciones arquitectónicas específicas:** La IA identificó con precisión que SQLite es un cuello de botella grave para la alta concurrencia requerida en inscripciones.
* **Fallas a nivel de infraestructura:** Identificó el peligro de fugas de recursos (thread pools bloqueados) si las excepciones de Spark no cierran correctamente las conexiones de base de datos.
* **Problemas de asincronismo:** Detectó que el uso del protocolo SMTP puede generar congelamientos en la pantalla si no se maneja de forma asíncrona.
* **Vulnerabilidades de sesión:** La IA advirtió sobre brechas de seguridad lógicas más allá de las contraseñas, como la manipulación de IDs en las URLs para ver datos de otros alumnos.

**Riesgos que encontró el equipo y la IA no:**
* **Factores humanos críticos:** El equipo contempló el riesgo real de abandono por parte de uno o más desarrolladores, algo común en entornos académicos.
* **Factores del entorno sociopolítico:** El equipo incluyó imprevistos externos de fuerza mayor, como paros, huelgas institucionales o quiebras.
* **Desconocimiento del dominio:** El equipo fue autocrítico al marcar como riesgo su propia falta de conocimiento sobre las reglas de negocio administrativas reales de una universidad.
* **Incompatibilidad de Hardware:** El equipo consideró posibles cuellos de botella físicos si los servidores reales de la institución son muy antiguos.

**Calidad del análisis:**
El análisis de la IA destacó por ser altamente técnico y específico al stack elegido (advirtiendo sobre los límites de SQLite y Spark) y aportó planes de mitigación accionables (como aplicar Unit Tests o patrones DAO). Por su parte, el análisis del equipo fue mucho más holístico y realista frente al contexto social y humano, evaluando amenazas extremas (paros, abandono de integrantes y falta de know-how institucional) que un modelo de lenguaje estandarizado tiende a omitir. La combinación de ambos resulta en una matriz de riesgos sumamente robusta.