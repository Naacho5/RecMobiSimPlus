# -*- coding: utf-8 -*-
"""
Created on Fri Mar 15 12:03:22 2024

@author: Ramón Hermoso
"""

import re
from collections import Counter

# Ruta al archivo que se va a analizar
file_path = 'output/item_floor_combined.txt'

# Inicializar un contador para almacenar las ocurrencias de cada número de habitación
room_counts = Counter()

# Abrir y leer el archivo. Se utiliza la codificación 'ISO-8859-1' para evitar errores de codificación
with open(file_path, 'r', encoding='ISO-8859-1') as file:
    for line in file:
        # Buscar las coincidencias con el patrón 'item_room_X=Y'
        match = re.search(r"item_room_(\d+)=(\d+)", line)
        if match:
            # Extraer el número de la habitación (valor Y) y aumentar su cuenta en el contador
            room = match.group(2)
            room_counts[room] += 1

# Imprimir los resultados
for room, count in room_counts.items():
    print(f'Habitación {room}: {count} elementos')

