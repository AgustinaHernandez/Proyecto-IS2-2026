#!/bin/bash
# -- mini-script para ejectuar la app --
# Dar permisos : chmod +x run.sh
# Ejecutar con : ./run.sh

#!/bin/bash

echo "======================================================="
echo "       INICIANDO SISTEMA DE GESTIÓN ACADÉMICA"
echo "======================================================="
echo ""

echo "[1/3] Limpiando y compilando el código Java..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo "[!] ERROR en la compilación."
    exit 1
fi

echo ""
echo "[2/3] Instrumentando modelos de base de datos (ActiveJDBC)..."
mvn process-classes
if [ $? -ne 0 ]; then
    echo "[!] ERROR al instrumentar los modelos."
    exit 1
fi

echo ""
echo "[3/3] Levantando el servidor web (Spark)..."
echo "======================================================="
echo "EL SERVIDOR ESTARÁ DISPONIBLE EN: http://localhost:8080"
echo "Presiona Ctrl+C para detenerlo."
echo "======================================================="
mvn exec:java -Dexec.mainClass="com.is1.proyecto.App"
