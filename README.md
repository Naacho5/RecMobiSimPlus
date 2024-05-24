<img src="/images/RecMobiSim.png" alt="RecMobiSim" width="300"/>

# Resumen
Simulador para la evaluación de CARS.

# Proyecto

"Next-gEnerATion dAta Management to foster suitable Behaviors and the resilience of cItizens against modErN ChallEnges (NEAT-AMBIENCE)", financiado por la Agencia Estatal de Investigación (PID2020-113037RB-I00 / AEI / 10.13039/501100011033). Investigador principal: Sergio Ilarri.

# Financiación

- Este trabajo se ha desarrollado como parte del proyecto de I+D+i PID2020-113037RB-I00, financiado por MCIN/AEI/ 10.13039/501100011033.
- Además del proyecto previo (proyecto NEAT-AMBIENCE), se agradece también el apoyo del Departamento de Ciencia, Universidad y Sociedad del Conocimiento del Gobierno de Aragón (Gobierno de Aragón: grupo COSMOS; última referencia del grupo: T64_23R).

# Página web

- [http://webdiis.unizar.es/~silarri/prot/RecMobiSim/](http://webdiis.unizar.es/~silarri/prot/RecMobiSim/)

- [http://webdiis.unizar.es/~silarri/NEAT-AMBIENCE/](http://webdiis.unizar.es/~silarri/NEAT-AMBIENCE/)

# Agradecimientos

- Álvaro Andrada (estudiante de la Universidad de Zaragoza) desarrolló el TFG relacionado "Gestión de escenarios para la evaluación de sistemas de recomendación", dirigido por Sergio Ilarri.

- Alejandro Piedrafita (estudiante de la Universidad de Zaragoza) desarrolló el TFG relacionado "Implementación de un Simulador para la Evaluación de Sistemas de Recomendación Dependientes del Contexto", dirigido por Sergio Ilarri.

# Notas

- El mapa que se carga por defecto al configurar e iniciar una simulación es el correspondiente a los ficheros de texto (graph_floor_combined.txt, item_floor_combined.txt, room_floor_combined.txt) que se encuentran en la carpeta resources/maps. Si se quiere utilizar un mapa distinto, basta copiar los ficheros .txt de la subcarpeta correspondiente (por ejemplo, resources/maps/GranCasa o resources/maps/MoMA_Museum) a la carpeta raíz de mapas (resources/maps).

- En algunos sistemas se ha observado un problema de codificación de caracteres en los ficheros fuentes al compilar. Puede resolverse cambiando la codificación a través de la variable de entorno JAVA_TOOL_OPTIONS (como alternativa, podría modificarse la codificación de los ficheros fuente problemáticos):

  `export JAVA_TOOL_OPTIONS=-Dfile.encoding=Cp1252`

- En la versión aquí registrada, si se genera el fichero .jar con el script "GenerateJar.sh", hay que incluir en dicho fichero .jar de forma manual el directorio resources/images. Probablemente esto sea fácilmente automatizable.

# Logos

<img src="/images/NEAT-AMBIENCE-logo.png" width="30%"> <img src="/images/NEAT-AMBIENCE-funder.png">

<img src="/images/cosmos-logo.png" width="30%">
<img src="/images/RecMobiSim.png" width="20%">
