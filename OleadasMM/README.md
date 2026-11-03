# OleadasMM - Oleadas infinitas con MythicMobs

Requisitos del servidor: Paper/Purpur 26.1.2 (Java 25), MythicMobs 5.x y (opcional pero recomendado) PlaceholderAPI.

## COMO COMPILARLO (sin saber programar)

### Opcion A: IntelliJ IDEA (en tu PC)
1. Instala IntelliJ IDEA Community (gratis): https://www.jetbrains.com/idea/download
2. Descomprime este zip. Deberia quedar una carpeta "OleadasMM" que contiene el archivo pom.xml.
3. En IntelliJ: File > Open > elige la carpeta OleadasMM > "Trust Project".
4. Espera a que abajo termine la barra de progreso (descarga las librerias con Maven).
   - Si te marca error de JDK: File > Project Structure > Project > SDK > Add SDK > Download JDK > version 25 (Temurin) > Download.
5. Abre el panel "Maven" (a la derecha) > OleadasMM > Lifecycle > doble clic en "package".
6. Cuando diga BUILD SUCCESS, el plugin esta en: OleadasMM/target/OleadasMM.jar

### Opcion B: GitHub (sin instalar nada)
1. Crea un repositorio en github.com y sube TODO el contenido de esta carpeta (incluida .github).
2. Pestaña Actions > build > Run workflow. Al terminar, descarga el artifact "OleadasMM" (dentro esta el .jar).

## INSTALACION
1. Pon OleadasMM.jar en la carpeta plugins/ del servidor (junto con MythicMobs y PlaceholderAPI).
2. Reinicia el servidor.
3. Edita plugins/OleadasMM/config.yml (nombres de mobs = nombre interno de MythicMobs) y haz /wave reload.

## COMO USARLO
1. /wave tool -> te da una vara. Click derecho en un bloque = añade un punto de spawn.
   Click izquierdo en un bloque = quita el punto de ese bloque. Se ven particulas en los puntos mientras tengas la vara.
2. /wave start -> inicia. /wave forceend -> termina y borra los mobs.
3. /wave set 70 -> salta a la oleada 70 y continua desde ahi.
4. Otros: /wave status, /wave reload, /wave spawns <list|remove n|clear|tp n>
Permiso: oleadas.admin (OP por defecto). Alias: /oleadas

Los mobs se limpian/borran solo en los mundos donde hay puntos de spawn.

## PLACEHOLDERS (PlaceholderAPI, identificador: oleadas)
Partida:
- %oleadas_wave%            oleada actual
- %oleadas_total_waves%     numero de la ultima oleada configurada
- %oleadas_status%          texto En curso / Detenida (editable en config)
- %oleadas_running%         true / false
- %oleadas_mobs_alive%      mobs de MythicMobs vivos en la arena
- %oleadas_mobs_spawned%    mobs ya spawneados en esta oleada
- %oleadas_mobs_amount%     mobs totales de esta oleada
- %oleadas_mobs_to_spawn%   mobs que faltan por salir
- %oleadas_next_wave_in%    segundos para la siguiente oleada
- %oleadas_next_wave_time%  igual pero en formato mm:ss

Jugador:
- %oleadas_player_wave%     wave mas alta alcanzada
- %oleadas_player_games%    partidas jugadas
- %oleadas_player_kills%    mobs de MythicMobs asesinados
- %oleadas_player_pos_wave%  / _pos_games / _pos_kills   su posicion en cada ranking

Clasificacion (para Toppers u otro):
- %oleadas_top_wave_1_name%   y   %oleadas_top_wave_1_value%
- %oleadas_top_kills_1_name%  y   %oleadas_top_kills_1_value%
- %oleadas_top_games_1_name%  y   %oleadas_top_games_1_value%
(cambia el 1 por la posicion 2, 3, 4...)

Los datos se guardan en plugins/OleadasMM/stats.yml y los puntos en spawnpoints.yml.
