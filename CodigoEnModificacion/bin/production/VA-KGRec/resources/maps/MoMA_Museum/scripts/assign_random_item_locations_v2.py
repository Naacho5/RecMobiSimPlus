# -*- coding: utf-8 -*-
"""
Created on Fri May 10 11:54:10 2024

@author: rhermoso
"""
import re
from shapely.geometry import Polygon, Point
import random

# Cargar datos de los archivos
room_data_path = '/mnt/data/room_floor_combined.txt'
item_data_path = '/mnt/data/item_floor_combined.txt'

def parse_polygon_data(lines):
    # Diccionario para almacenar los vértices de cada sala
    polygons = {}
    vertex_pattern = re.compile(r'room_corner_xy_(\d+)_(\d+)=(\d+\.\d+), (\d+\.\d+)')

    for line in lines:
        match = vertex_pattern.match(line)
        if match:
            n = match.group(1)  # Número del vértice
            r = match.group(2)  # Número de la sala
            x = float(match.group(3))
            y = float(match.group(4))
            if r not in polygons:
                polygons[r] = []
            polygons[r].append((x, y))

    # Asegurarse de que los vértices estén en el orden correcto y cerrar el polígono
    for r in polygons:
        polygons[r].sort()  # Sort by x then by y
        polygons[r].append(polygons[r][0])  # Close the polygon

    return polygons

def parse_room_assignments(lines):
    room_assignments = {}
    assignment_pattern = re.compile(r'item_room_(\d+)=(\d+)')

    for line in lines:
        match = assignment_pattern.match(line)
        if match:
            item_id = int(match.group(1))
            room_id = match.group(2)
            if room_id not in room_assignments:
                room_assignments[room_id] = []
            room_assignments[room_id].append(item_id)

    return room_assignments

def generate_random_points(polygons, room_assignments):
    points = []
    for room_id in room_assignments:
        if room_id in polygons:  # Verificar que el polígono esté definido
            poly = Polygon(polygons[room_id])
            for _ in range(50):  # Generar 50 puntos por sala
                while True:
                    random_point = Point(random.uniform(poly.bounds[0], poly.bounds[2]), random.uniform(poly.bounds[1], poly.bounds[3]))
                    if poly.contains(random_point):
                        points.append(f"vertex_xy_{room_id}={random_point.x},{random_point.y}")
                        break
    return points

# Leer datos
with open(room_data_path, 'r') as file:
    room_lines = file.readlines()
with open(item_data_path, 'r') as file:
    item_lines = file.readlines()

# Procesar datos
polygons = parse_polygon_data(room_lines)
room_assignments = parse_room_assignments(item_lines)

# Generar puntos aleatorios
random_points = generate_random_points(polygons, room_assignments)
random_points[:10]  # Mostrar los primeros 10 puntos para revisión
