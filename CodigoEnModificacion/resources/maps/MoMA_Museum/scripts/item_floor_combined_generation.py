# -*- coding: utf-8 -*-
"""
Created on Fri Mar 15 10:26:03 2024

@author: Ramón Hermoso
"""

import pandas as pd

# Paso 1: Leer el archivo CSV
gen_df = pd.read_csv('./generated_ratings_with_item_info.csv')

# Asegurarse de que no hay duplicados para un mismo ObjectID con respecto a los atributos relevantes
unique_items = gen_df[['ObjectID', 'Date', 'Title', 'floor', 'room']].drop_duplicates()

# Contar el número total de ObjectID distintos
number_items = len(unique_items['ObjectID'].unique())

# Paso 3: Generar el contenido del archivo, incluyendo el número total de items al principio
output_lines = [f"number_items={number_items}\n", "name=Painting and Sculpture I and II\n"]

# Agregar una única vez la línea vertex_dimension_width
output_lines.append("vertex_dimension_width=20\n")

for _, row in unique_items.iterrows():
    object_id = row['ObjectID']
    date = row['Date']
    title = row['Title']
    # floor = row['floor']
    room = row['room']
    
    output_lines.extend([
        f"item_beginDate_{object_id}={date}\n",
        f"item_endDate_{object_id}={date}\n",
        f"item_width_{object_id}=66.0\n",
        f"vertex_label_{object_id}=Painting\n",
        f"item_itemID_{object_id}={object_id}\n",
        f"vertex_url_{object_id}=\\resources\\images\\sculpture.png\n",
        # Asegúrate de ajustar las coordenadas según los datos de tu archivo 'item_floor_combined.txt'
        f"vertex_xy_{object_id}=217.8485915492958, 1032.4730716109657\n",
        f"item_nationality_{object_id}=American\n",
        f"item_title_{object_id}={title}\n",
        # f"item_floor_{object_id}={floor}\n",
        f"item_room_{object_id}={room-400}\n"
    ])

# Paso 4: Escribir el contenido al archivo items.txt
with open('output/item_floor_combined.txt', 'w') as f:
    f.writelines(output_lines)

