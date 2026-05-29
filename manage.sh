#!/bin/bash

# --- Paleta de Colores ---
VERDE='\033[0;32m'
CIAN='\033[0;36m'
ROJO='\033[0;31m'
AMARILLO='\033[1;33m'
BLANCO='\033[1;37m'
RESET='\033[0m'

build_app() {
    echo -e "${CIAN}[*] Construyendo el proyecto e instrumentando ActiveJDBC...${RESET}"
    mvn clean compile activejdbc-instrumentation:instrument
    if [ $? -eq 0 ]; then
        echo -e "${VERDE}[+] Build completado con éxito.${RESET}"
        return 0
    else
        echo -e "${ROJO}[!] Error en el proceso de build. Revisa los logs de Maven.${RESET}"
        return 1
    fi
}

run_app() {
    echo -e "${CIAN}[*] Iniciando el servidor Spark...${RESET}"
    echo -e "${VERDE}[+] El sistema estará disponible en: http://localhost:8080${RESET}"
    mvn compile activejdbc-instrumentation:instrument exec:java -Dexec.mainClass="com.is1.proyecto.App"
}

crun_app() {
    build_app
    # Solo ejecuta run_app si el build_app devolvió 0 (éxito)
    if [ $? -eq 0 ]; then
        echo -e "${CIAN}[*] Build exitoso. Lanzando aplicación...${RESET}"
        echo ""
        run_app
    else
        echo -e "${ROJO}[!] Ejecución cancelada debido a errores de compilación.${RESET}"
    fi
}

init_db() {
    echo -e "${AMARILLO}[!] ATENCIÓN: Se recreará la base de datos desde cero.${RESET}"
    
    if command -v sqlite3 &> /dev/null; then
        rm -f proyecto.db
        echo -e "${CIAN}[*] Generando estructura (schema.sql)...${RESET}"
        sqlite3 proyecto.db < src/main/resources/scheme.sql
        echo -e "${CIAN}[*] Inyectando datos iniciales (data.sql)...${RESET}"
        sqlite3 proyecto.db < src/main/resources/data.sql
        echo -e "${VERDE}[+] Base de datos (proyecto.db) inicializada correctamente.${RESET}"
    else
        echo -e "${ROJO}[!] ERROR: La herramienta 'sqlite3' no está instalada en tu sistema.${RESET}"
    fi
}

run_tests() {
    echo -e "${CIAN}[*] Ejecutando entorno de pruebas...${RESET}"
    mvn test
}

show_help() {
    echo -e "${CIAN}"
    echo "  ___ ___ ___    ___  ___  ___       _ ___ ___ _____ "
    echo " |_ _/ __|_  )  | _ \\| _ \\/ _ \\ _ _ | | __/ __|_   _|"
    echo "  | |\\__ \\/ /   |  _/|   / (_) | || | | _| (__  | |  "
    echo " |___|___/___|  |_|  |_|_\\\\___/ \\_, |_|___\\___| |_|  "
    echo "                                |__/                 "
    echo -e "${BLANCO}        SISTEMA DE GESTIÓN ACADÉMICA - CLI v1.1      ${RESET}"
    echo -e "${CIAN}=====================================================${RESET}"
    echo -e "Uso recomendado: ${AMARILLO}./proyecto.sh [comando]${RESET}"
    echo ""
    echo -e "${BLANCO}Comandos disponibles:${RESET}"
    printf "  ${VERDE}%-15s${RESET} %s\n" "compile" "Limpia y compila el código (ActiveJDBC Instrument)."
    printf "  ${VERDE}%-15s${RESET} %s\n" "run" "Levanta el servidor web en localhost:8080."
    printf "  ${VERDE}%-15s${RESET} %s\n" "crun" "Compila y luego ejecuta el servidor (Build + Run)."
    printf "  ${VERDE}%-15s${RESET} %s\n" "reset_db" "Formatea y recarga la base de datos con datos de prueba."
    printf "  ${VERDE}%-15s${RESET} %s\n" "test" "Ejecuta los tests unitarios del sistema."
    printf "  ${VERDE}%-15s${RESET} %s\n" "help" "Muestra este panel de ayuda."
    echo ""
}

# --- Router de comandos ---
case "$1" in
    "compile") build_app ;;
    "run") run_app ;;
    "crun") crun_app ;;
    "reset_db") init_db ;;
    "test") run_tests ;;
    *) show_help ;;
esac
