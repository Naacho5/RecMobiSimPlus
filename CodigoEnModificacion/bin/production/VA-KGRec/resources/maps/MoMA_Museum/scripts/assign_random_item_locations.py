# -*- coding: utf-8 -*-
"""
Created on Thu May  2 22:10:27 2024

@author: rhermoso
"""

import re
import random
from shapely.geometry import Polygon, Point

def sort_vertices_into_rectangle(vertices):
    # Ordenar los vértices por x, luego por y para formar un rectángulo
    sorted_by_x = sorted(vertices, key=lambda x: (x[0], x[1]))
    lower = sorted(sorted_by_x[:2], key=lambda x: x[1])
    upper = sorted(sorted_by_x[2:], key=lambda x: x[1], reverse=True)
    return lower + upper  # Formar el rectángulo

def parse_data_from_file(filename):
    polygons = {}
    
    vertex_pattern = re.compile(r'room_corner_xy_(\d+)_(\d+)=(\d+\.\d+), (\d+\.\d+)')

    with open(filename, 'r') as file:
        for line in file:
            vertex_match = vertex_pattern.match(line.strip())
            if vertex_match:
                room_id = vertex_match.group(2)
                x = float(vertex_match.group(3))
                y = float(vertex_match.group(4))
                
                if room_id not in polygons:
                    polygons[room_id] = []
                
                polygons[room_id].append((x, y))

    # Asegurarse de que cada polígono es un rectángulo
    for room_id in polygons:
        polygons[room_id] = sort_vertices_into_rectangle(polygons[room_id])
    
    return polygons

# Item counts provided directly
item_counts = {
    '1': 38, '10': 31, '11': 34, '12': 36, '13': 33, '14': 36, '15': 26,
    '16': 21, '17': 25, '18': 30, '19': 34, '2': 34, '20': 35, '21': 22,
    '22': 0, '23': 0, '24': 0, '25': 0, '26': 0, '3': 25, '4': 13,
    '5': 25, '6': 32, '7': 40, '8': 31, '9': 34
}

def generate_random_points(polygons, item_counts):
    points = {}
    index = 1  # Iniciar el contador de índices
    outputs = []
    
    for room_id, coords in polygons.items():
        num_points = item_counts.get(room_id, 0)
        if num_points > 0:
            poly = Polygon(coords)
            for _ in range(num_points):
                while True:
                    point = Point(random.uniform(poly.bounds[0], poly.bounds[2]), 
                                  random.uniform(poly.bounds[1], poly.bounds[3]))
                    if poly.contains(point):
                        points.setdefault(room_id, []).append(point)
                        outputs.append(f"vertex_xy_{index} = {point.x}, {point.y}")
                        index += 1
                        break

    return outputs

# Path to your data file
filename = 'output/room_floor_combined.txt'

# Parse the polygons from the file
polygons = parse_data_from_file(filename)

# Generate random points inside the polygons and format them
formatted_points = generate_random_points(polygons, item_counts)

# Output the results
for line in formatted_points:
    print(line)
