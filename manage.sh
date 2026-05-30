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
    if [ $? -eq 0 ]; then
        echo -e "${CIAN}[*] Build exitoso. Lanzando aplicación...${RESET}"
        echo ""
        run_app
    else
        echo -e "${ROJO}[!] Ejecución cancelada debido a errores de compilación.${RESET}"
    fi
}

# Recibe 2 parámetros: $1 = Ruta de la BD, $2 = "full" (carga datos) o "schema" (solo tablas)
init_db() {
    local DB_PATH=$1
    local LOAD_MODE=$2

    echo -e "${AMARILLO}[!] Inicializando base de datos en: ${DB_PATH}${RESET}"
    
    if command -v sqlite3 &> /dev/null; then
        mkdir -p "$(dirname "$DB_PATH")"
        rm -f "$DB_PATH"
        echo -e "${CIAN}[*] Generando estructura (scheme.sql)...${RESET}"
        sqlite3 "$DB_PATH" < src/main/resources/scheme.sql
        
        if [ "$LOAD_MODE" == "full" ]; then
            echo -e "${CIAN}[*] Inyectando datos iniciales (data.sql)...${RESET}"
            sqlite3 db/dev.db < src/main/resources/data.sql
        fi
        echo -e "${VERDE}[+] Base de datos inicializada correctamente.${RESET}"
    else
        echo -e "${ROJO}[!] ERROR: La herramienta 'sqlite3' no está instalada en tu sistema.${RESET}"
    fi
}

run_tests() {
    init_db "target/test.db" "schema"
    echo -e "${CIAN}[*] Ejecutando entorno de pruebas...${RESET}"
    mvn test -Ptest
}

# ------------------------------------------------------------------------------
# NUEVO: Descarga de credenciales
# ------------------------------------------------------------------------------
download_credentials() {
    echo -e "${CIAN}[*] Solicitando credenciales al servidor seguro...${RESET}"
    
    # Pedir contraseña de forma silenciosa
    read -s -p "Ingrese la clave secreta: " secret_pass
    echo "" # Salto de línea por prolijidad

    local API_URL="https://tgofdrive.duckdns.org/proyecto_is/api/get_credentials"
    local CREDS_FILE="credentials.json"

    # Ejecutamos curl de forma silenciosa
    HTTP_STATUS=$(curl -s -w "%{http_code}" -o "$CREDS_FILE" -H "X-Secret-Pass: $secret_pass" "$API_URL")

    # Evaluamos la respuesta de Nginx
    if [ "$HTTP_STATUS" -eq 200 ]; then
        echo -e "${VERDE}[+] ¡Éxito! Credenciales guardadas en: $(pwd)/$CREDS_FILE${RESET}"
    elif [ "$HTTP_STATUS" -eq 403 ] || [ "$HTTP_STATUS" -eq 401 ]; then
        echo -e "${ROJO}[!] Error: Contraseña incorrecta. Acceso denegado.${RESET}"
        rm -f "$CREDS_FILE" # Limpiamos el archivo residual
    else
        echo -e "${ROJO}[!] Error inesperado del servidor. Código HTTP: $HTTP_STATUS${RESET}"
        rm -f "$CREDS_FILE"
    fi
}

show_help() {
    echo -e "${CIAN}"
    echo "  ___ ___ ___    ___  ___  ___       _ ___ ___ _____ "
    echo " |_ _/ __|_  )  | _ \\| _ \\/ _ \\ _ _ |  __/ __|_   _|"
    echo "  | |\\__ \\/ /   |  _/|   / (_) | || |  _| (__  | |  "
    echo " |___|___/___|  |_|  |_|_\\\\___/ \\_, |____\\___| |_|  "
    echo "                                |__/                 "
    echo -e "${BLANCO}        SISTEMA DE GESTIÓN ACADÉMICA - CLI v1.1      ${RESET}"
    echo -e "${CIAN}=====================================================${RESET}"
    echo -e "Uso recomendado: ${AMARILLO}./manage.sh [comando]${RESET}"
    echo ""
    echo -e "${BLANCO}Comandos disponibles:${RESET}"
    printf "  ${VERDE}%-15s${RESET} %s\n" "compile" "Limpia y compila el código (ActiveJDBC Instrument)."
    printf "  ${VERDE}%-15s${RESET} %s\n" "run" "Levanta el servidor web en localhost:8080."
    printf "  ${VERDE}%-15s${RESET} %s\n" "crun" "Compila y luego ejecuta el servidor (Compile + Run)."
    printf "  ${VERDE}%-15s${RESET} %s\n" "reset_db" "Formatea y recarga la base de datos con datos de prueba."
    printf "  ${VERDE}%-15s${RESET} %s\n" "test" "Ejecuta los tests unitarios del sistema."
    printf "  ${VERDE}%-15s${RESET} %s\n" "get_creds" "Descarga el JSON de credenciales desde el servidor."
    printf "  ${VERDE}%-15s${RESET} %s\n" "help" "Muestra este panel de ayuda."
    echo ""
}

# --- Router de comandos ---
case "$1" in
    "compile") build_app ;;
    "run") run_app ;;
    "crun") crun_app ;;
    "reset_db") init_db "db/dev.db" "full" ;;
    "test") run_tests ;;
    "get_creds") download_credentials ;;
    *) show_help ;;
esac