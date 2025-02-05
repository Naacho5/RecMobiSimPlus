# -*- coding: utf-8 -*-
"""
Created on Tue Jul 23 12:06:33 2024

@author: rhermoso
"""

import pandas as pd
import sqlite3
import os
import time

# Cargar el archivo CSV
file_path = os.path.join('..', 'RecMobiSim', 'SimulatorAndMapEditor', 'resources', 'maps', 'MoMA_Museum', 'items_ratings_predicted.csv')

df = pd.read_csv(file_path)

# Conectar a la base de datos SQLite
db_path = os.path.join('..', 'RecMobiSim', 'SimulatorAndMapEditor', 'resources', 'db', 'db_museum.db') 

# Intentar conectar múltiples veces en caso de bloqueo
attempts = 5
for attempt in range(attempts):
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        # Borrar el contenido de la tabla user_item_context
        cursor.execute('DELETE FROM user_item_context')

        # Insertar datos en la tabla user_item_context
        for _, row in df.iterrows():
            cursor.execute('''
                INSERT INTO user_item_context (id_user, id_item, id_context, rating, opinion, user_provided)
                VALUES (?, ?, 1, ?, NULL, 1)
            ''', (row['UserId'], row['Id'], row['rating']))

        # Confirmar los cambios
        conn.commit()
        break
    except sqlite3.OperationalError as e:
        if "database is locked" in str(e):
            if attempt < attempts - 1:
                time.sleep(2)  # Esperar antes de reintentar
            else:
                raise
    finally:
        # Cerrar el cursor y la conexión
        cursor.close()
        conn.close()
