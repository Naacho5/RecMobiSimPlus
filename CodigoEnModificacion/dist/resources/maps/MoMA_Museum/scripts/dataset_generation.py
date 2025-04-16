# -*- coding: utf-8 -*-
"""
Created on Wed Mar 13 13:18:59 2024

@author: Ramón Hermoso
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# Carga de los datos
users = pd.read_csv('input/users_original.csv')
items = pd.read_csv('input/items_original.csv', encoding='utf-8', delimiter=';', error_bad_lines=False, warn_bad_lines=True)

# Función para generar ratings
def generate_ratings(users, items, noise_level=0.1, sparsity = 0.1):
    # Preparar el DataFrame de resultados
    ratings = pd.DataFrame(columns=['UserId', 'ObjectID', 'rating'])
    
    # Iterar sobre cada usuario
    for user_id in users['UserId'].unique():
        # Seleccionar ítems al azar para este usuario (puedes ajustar la cantidad)
        selected_items = items.sample(frac=1-sparsity, random_state=int(user_id))  # Ejemplo: seleccionar el 20% de los items para cada usuario
        
        # Generar ratings para los ítems seleccionados
        for index, item in selected_items.iterrows():
            # Inicializar el rating base en función del 'style' y 'genre'
            base_rating = np.random.choice(range(2, 5))  # Preferencia base en [2..5]
            
            # Añadir ruido
            noise = np.random.normal(0, noise_level)  # El ruido sigue una normal centrada en 0
            rating = base_rating + noise
            
            # Asegurar que el rating final esté entre 1 y 5
            final_rating = np.clip(round(rating), 1, 5)
            
            # Añadir el rating generado al DataFrame
            ratings = ratings.append({'UserId': user_id, 'ObjectID': item['ObjectID'], 'rating': final_rating}, ignore_index=True)
    
    return ratings

# Generar los ratings con un cierto nivel de ruido
noise_level = 0.2
ratings = generate_ratings(users, items, noise_level)

# Unir los ratings generados con los atributos de los ítems
ratings_with_item_info = pd.merge(ratings, items, on='ObjectID', how='left')

# Guardamos los ratings generados con información de ítems en un nuevo archivo CSV
ratings_with_item_info.to_csv('generated_ratings_with_item_info.csv', index=False)

print(ratings_with_item_info.head())

# Generamos un informe con las medias y desviaciones típicas de los usuarios agrupados por style y genre
stats_by_user_genre_style = ratings_with_item_info.groupby(['UserId', 'genre', 'style'])['rating'].agg(['mean', 'std']).reset_index()

print('********* Información agregada (style y genre) *********')
print(stats_by_user_genre_style)


print('********* Información por style y genre por separado *********')
# Media y desviación estándar por usuario y genre
stats_by_user_genre = ratings_with_item_info.groupby(['UserId', 'genre'])['rating'].agg(['mean', 'std']).reset_index()

# Media y desviación estándar por usuario y style
stats_by_user_style = ratings_with_item_info.groupby(['UserId', 'style'])['rating'].agg(['mean', 'std']).reset_index()

print("Estadísticas por genre:")
print(stats_by_user_genre.head())
print("\nEstadísticas por style:")
print(stats_by_user_style.head())

########### Lo mostramos gráficamente (información por separado)

# Configuración de la visualización para Genre
# plt.figure(figsize=(14, 7))
# plt.subplot(1, 2, 1)  # Gráfica 1 de 2 en una disposición 1x2

# # Asumiendo que queremos visualizar los datos para un conjunto limitado de usuarios por claridad
# subset_users = stats_by_user_genre['UserId'].unique()[:5]  # Tomar los primeros 5 usuarios como ejemplo

# data_for_subset_users_genre = stats_by_user_genre[stats_by_user_genre['UserId'].isin(subset_users)]

# sns.barplot(x='UserId', y='mean', hue='genre', data=data_for_subset_users_genre, ci='sd', palette='viridis')
# plt.title('Mean Ratings by Genre for Each User')
# plt.xlabel('User ID')
# plt.ylabel('Mean Rating')
# plt.xticks(rotation=45)
# plt.legend(title='Genre', bbox_to_anchor=(1.05, 1), loc='upper left')

# # Configuración de la visualización para Style
# plt.subplot(1, 2, 2)  # Gráfica 2 de 2 en la misma fila

# data_for_subset_users_style = stats_by_user_style[stats_by_user_style['UserId'].isin(subset_users)]

# sns.barplot(x='UserId', y='mean', hue='style', data=data_for_subset_users_style, ci='sd', palette='coolwarm')
# plt.title('Mean Ratings by Style for Each User')
# plt.xlabel('User ID')
# plt.ylabel('Mean Rating')
# plt.xticks(rotation=45)
# plt.legend(title='Style', bbox_to_anchor=(1.05, 1), loc='upper left')

# plt.tight_layout()
# plt.show()





