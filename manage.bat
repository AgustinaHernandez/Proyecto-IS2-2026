@echo off
title IS2 Project CLI
color 0B

if "%1"=="compile" goto build_app
if "%1"=="run" goto run_app
if "%1"=="crun" goto crun_app
if "%1"=="reset_db" goto init_db
if "%1"=="test" goto run_tests
goto show_help

:build_app
echo [*] Construyendo el proyecto e instrumentando ActiveJDBC...
call mvn clean compile activejdbc-instrumentation:instrument
if %ERRORLEVEL% EQU 0 (
    echo [+] Build completado con exito.
    exit /b 0
) else (
    echo [!] Error en el proceso de build. Revisa los logs de Maven.
    exit /b 1
)

:run_app
echo [*] Iniciando el servidor Spark...
echo [+] El sistema estara disponible en: http://localhost:8080
call mvn compile activejdbc-instrumentation:instrument exec:java -Dexec.mainClass="com.is1.proyecto.App"
goto end

:crun_app
echo [*] Iniciando proceso CRUN (Build + Run)...
call :build_app
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [*] Build exitoso. Lanzando aplicacion...
    goto run_app
) else (
    echo.
    echo [!] Ejecucion cancelada debido a errores de compilacion.
)
goto end

:init_db
echo [!] ATENCION: Se recreara la base de datos desde cero.
if exist proyecto.db del proyecto.db
echo [*] Generando estructura (schema.sql)...
sqlite3 proyecto.db < schema.sql
echo [*] Inyectando datos iniciales (data.sql)...
sqlite3 proyecto.db < data.sql
echo [+] Base de datos (proyecto.db) inicializada correctamente.
goto end

:run_tests
echo [*] Ejecutando entorno de pruebas...
call mvn test
goto end

:show_help
echo.
echo   ___ ___ ___    ___  ___  ___       _ ___ ___ _____ 
echo  ^|_ _/ __^|_  )  ^| _ \^| _ \/ _ \ _ _ ^| ^| __/ __^|_   _^|
echo   ^| ^|\__ \/ /   ^|  _^/^|   / (_) ^| ^|^| ^| ^| _^| (__  ^| ^|  
echo  ^|___^|___/___^|  ^|_^|  ^|_^|_\___/ \_, ^|_^|___\___^| ^|_^|  
echo                                 ^|__/                 
echo         SISTEMA DE GESTION ACADEMICA - CLI v1.1      
echo =====================================================
echo Uso recomendado: proyecto.bat [comando]
echo.
echo Comandos disponibles:
echo   compile  - Limpia y compila el codigo (ActiveJDBC).
echo   run      - Levanta el servidor web (localhost:8080).
echo   crun     - Compila y luego ejecuta el servidor (Build + Run).
echo   reset_db - Formatea y recarga la base de datos con datos de prueba.
echo   test     - Ejecuta los tests unitarios del sistema.
echo   help     - Muestra este panel de ayuda.
echo.
goto end

:end
