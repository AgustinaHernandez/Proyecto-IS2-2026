# Sistema de gestión estudiantil


Proyecto académico desarrollado actualmente para la materia **Ingeniería de Software II**. Su objetivo principal es diseñar y documentar un **Sistema de Gestión Estudiantil**.
Este repositorio representa la evolución continua del sistema, habiendo iniciado su desarrollo en 2025 como parte de la materia **Ingeniería de Software I**.


## 📋 Requisitos Previos
Para poder ejecutar este proyecto en tu máquina local, necesitas tener instalado:
* **Java JDK 11** (o superior).
* **Maven** (Asegúrate de tenerlo agregado a las variables de entorno / PATH).
* **SQLite** (O una extensión en tu editor como *SQLite Viewer* para ejecutar los scripts).

## 🚀 Instalación y Ejecución

### Paso 1: Base de Datos
El proyecto utiliza SQLite. Para inicializar la base de datos con datos de prueba, debes:
1. Crear un archivo vacío llamado `proyecto.db` (o el nombre que figure en tu `DBConfigSingleton`).
2. Ejecutar el archivo `schema.sql` sobre esa base de datos para crear las tablas.
3. Ejecutar el archivo `data.sql` para insertar los usuarios y materias de prueba.

### Paso 2: Ejecutar el Proyecto
Para compilar, instrumentar los modelos de ActiveJDBC y levantar el servidor web, simplemente haz doble clic o ejecuta en terminal el script correspondiente a tu sistema operativo:

- **En Windows:**
  Doble clic en el archivo `run.bat` o ejecuta en consola:
  ```cmd
  .\run.bat
  ```


- **En Linux / Mac:**
    Dale permisos de ejecución al script y córrelo:
    ```bash
    chmod +x run.sh
    ./run.sh
    ```

### Paso 3: Uso

Una vez que la consola indique que Spark ha iniciado, abre tu navegador web y dirígete a:
👉 **http://localhost:8080**







## Objetivo del sistema
Centralizar la información académica de estudiantes, docentes y materias, facilitando la gestión administrativa, inscripciones, exámenes y seguimiento académico.

## Estructura del repositorio
- `db/` → Bases de datos de desarrollo y producción.
- `documentacion/` → Contiene la documentación en PDF.
- `META-INF/` → Metadatos del proyecto.
- `src/main/java/config` → Configuración única de conexión a la base de datos.
- `src/main/java/models` → Clases (modelos) para mapeo de la ORM a las tablas de la base de datos.
- `src/main/java/App.java` → Aplicación principal que contiene la lógica de funcionamiento.
- `src/main/resources/scheme.sql` → Script para inicializar la base de datos.
- `src/main/resources/templates` → Plantillas web de mustache, que le dan estilo gráfico a la app.
- `src/test` → Pruebas de unidad sobre App.
- `target` → Archivos de compilación, reportes y propiedades.
- `LICENSE` → Licencia Apache.
- `pom.xml` → Archivo de configuración de proyecto Maven.
- `README.md` → descripción breve del proyecto.


## Equipo (nro 7)
- [adrb1806 | ](https://github.com/adrb1806) Barone, Adrian  
- [AgustinaHernandez | ](https://github.com/AgustinaHernandez) Hernández, Agustina
- [C0dexDev | ](https://github.com/C0dexDev) Menéndez, José Ignacio
- [franpelliciotti | ](https://github.com/franpelliciotti) Pagliasso, Francine

## Versiones del proyecto
- [**Versión 2025 (guardada)**](https://github.com/C0dexDev/proyecto_is1_grupo7_c1)
- [**Versión 2026 (En trabajo)**](https://github.com/AgustinaHernandez/Proyecto-IS2-2026)

---
📖 **Universidad Nacional de Rio Cuarto - 2026**
