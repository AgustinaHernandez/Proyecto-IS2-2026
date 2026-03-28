#!/bin/bash
# script para resetear la base  de datos
# 	-borra dev.db
#  	-vuelve a cargar scheme.sql y data.sql


if [ -f "db/dev.db" ]; then
    rm db/dev.db 
fi

sqlite3 db/dev.db  < src/main/resources/scheme.sql

sqlite3 db/dev.db < src/main/resources/data.sql


		


