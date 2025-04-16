import os
import re

# Path to the file to be modified
file_path = 'output/item_floor_combined.txt'

# Ensure the directory exists
os.makedirs(os.path.dirname(file_path), exist_ok=True)

# Function to process lines and update IDs based on patterns
def process_lines(lines):
    item_id_counter = 1
    item_id_map = {}
    new_lines = []
    item_id_pattern = re.compile(r'^item_itemID_(\d+)=(\d+)')

    # First pass: update item_itemID lines and map old IDs to new sequential IDs
    for line in lines:
        match = item_id_pattern.match(line)
        if match:
            old_id = match.group(1)
            value = match.group(2)
            new_line = f'item_itemID_{item_id_counter}={value}'
            item_id_map[old_id] = item_id_counter
            item_id_counter += 1
            new_lines.append(new_line + '\n')
        else:
            new_lines.append(line)

    # Second pass: update other item and vertex lines using the map
    final_lines = []
    # Pattern to catch all other item and vertex lines with an old ID
    item_vertex_pattern = re.compile(r'^(item_|vertex_)([a-zA-Z]+_)(\d+)(=.+)$')
    for line in new_lines:
        match = item_vertex_pattern.match(line)
        if match:
            prefix = match.group(1) + match.group(2)  # 'item_' or 'vertex_' + 'label_', 'url_', 'xy_', etc.
            old_id = match.group(3)
            suffix = match.group(4)
            if old_id in item_id_map:
                new_line = f'{prefix}{item_id_map[old_id]}{suffix}\n'
                final_lines.append(new_line)
            else:
                final_lines.append(line)
        else:
            final_lines.append(line)

    return final_lines

# Read the file content
try:
    with open(file_path, 'r') as file:
        lines = file.readlines()
except FileNotFoundError:
    print("El fichero especificado no se encontró.")
    exit()

# Process the lines to update IDs
processed_lines = process_lines(lines)

for l in processed_lines:
    print(l)

# print(processed_lines)

# # Path to the output file
# aux_file_path = 'output/aux.txt'

# # Write the processed content back to a new file
# try:
#     with open("output/aux.txt", 'w') as file:
#         c = 1
#         for line in processed_lines:
#             # print(line)
#         # file.writelines(processed_lines)
#             if c % 100 == 0:
#                 print('*** ' + str(c))
#             c += 1
#             file.write(line)
#     file.close()
    
# except IOError as e:
#     print(f"Error al escribir en el fichero: {e}")
    



















# import os
# import re

# # Path to the file to be modified
# file_path = 'output/item_floor_combined.txt'

# # Ensure the directory exists
# os.makedirs(os.path.dirname(file_path), exist_ok=True)

# # Function to process lines and update IDs
# def process_lines(lines):
#     item_id_counter = 1
#     item_id_map = {}
#     new_lines = []
#     begin_date_pattern = re.compile(r'^item_beginDate_(\d+)=(\d+)')
#     item_id_pattern = re.compile(r'^item_itemID_(\d+)=(\d+)')
    
#     # First pass: update item_itemID lines and map old IDs to new sequential IDs
#     for line in lines:
#         if item_id_pattern.match(line):
#             old_id = item_id_pattern.match(line).group(1)
#             value = item_id_pattern.match(line).group(2)
#             new_id = f'item_itemID_{item_id_counter}={value}'
#             item_id_map[old_id] = item_id_counter
#             item_id_counter += 1
#             new_lines.append(new_id + '\n')
#         else:
#             new_lines.append(line)
    
#     # Second pass: update item_beginDate lines using the map
#     final_lines = []
#     for line in new_lines:
#         match = begin_date_pattern.match(line)
#         if match:
#             old_id = match.group(1)
#             value = match.group(2)
#             if old_id in item_id_map:
#                 new_line = f'item_beginDate_{item_id_map[old_id]}={value}\n'
#                 final_lines.append(new_line)
#             else:
#                 final_lines.append(line)
#         else:
#             final_lines.append(line)
    
#     return final_lines

# # Read the file content
# try:
#     with open(file_path, 'r') as file:
#         lines = file.readlines()
# except FileNotFoundError:
#     print("El fichero especificado no se encontró.")
#     exit()

# # Process the lines to update IDs
# processed_lines = process_lines(lines)

# print(processed_lines)

# # Path to the output file
# # aux_file_path = 'output/aux.txt'

# # Write the processed content back to a new file
# # try:
# #     with open(aux_file_path, 'w') as file:
# #         file.writelines(processed_lines)
# # except IOError as e:
# #     print(f"Error al escribir en el fichero: {e}")
