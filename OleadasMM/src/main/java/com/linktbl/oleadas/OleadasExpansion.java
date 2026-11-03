package com.linktbl.oleadas;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;

/**
 * Placeholders (identificador: oleadas)
 */
public class OleadasExpansion extends PlaceholderExpansion {

    private final OleadasPlugin plugin;

    public OleadasExpansion(OleadasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "oleadas";
    }

    @Override
    public String getAuthor() {
        return "LINKTBL";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String p = params.toLowerCase(Locale.ROOT);
        WaveManager w = plugin.waves();
        StatsManager st = plugin.stats();

        switch (p) {
            case "wave": return String.valueOf(w.getCurrentWave());
            case "total_waves": return String.valueOf(w.getTotalWaves());
            case "running": return String.valueOf(w.isRunning());
            case "status":
                return plugin.color(plugin.getConfig().getString(
                        w.isRunning() ? "placeholders.status-running" : "placeholders.status-stopped",
                        w.isRunning() ? "En curso" : "Detenida"));
            case "mobs_alive": return String.valueOf(w.getAlive());
            case "mobs_spawned": return String.valueOf(w.getSpawnedInWave());
            case "mobs_amount": return String.valueOf(w.getWaveAmount());
            case "mobs_to_spawn": return String.valueOf(w.getToSpawn());
            case "next_wave_in": return String.valueOf(w.getSecondsToNext());
            case "next_wave_time": {
                int sec = w.getSecondsToNext();
                return String.format("%02d:%02d", sec / 60, sec % 60);
            }
            default:
                break;
        }

        // %oleadas_top_<wave|kills|games>_<posicion>_<name|value>%
        if (p.startsWith("top_")) {
            String[] a = p.split("_");
            if (a.length != 4) return null;
            int pos;
            try {
                pos = Integer.parseInt(a[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            List<StatsManager.PlayerStats> list = st.top(a[1]);
            boolean wantName = a[3].equals("name");
            if (pos < 1 || pos > list.size()) {
                return wantName ? plugin.getConfig().getString("placeholders.empty-name", "---") : "0";
            }
            StatsManager.PlayerStats s = list.get(pos - 1);
            return wantName ? s.name : String.valueOf(StatsManager.value(s, a[1]));
        }

        // Datos del jugador
        if (p.startsWith("player_") && player != null) {
            String f = p.substring(7);
            StatsManager.PlayerStats s = st.get(player.getUniqueId());
            String none = plugin.getConfig().getString("placeholders.empty-position", "-");
            switch (f) {
                case "wave": return String.valueOf(s == null ? 0 : s.highestWave);
                case "games": return String.valueOf(s == null ? 0 : s.games);
                case "kills": return String.valueOf(s == null ? 0 : s.kills);
                case "pos_wave": {
                    int pos = st.position(player.getUniqueId(), "wave");
                    return pos == 0 ? none : String.valueOf(pos);
                }
                case "pos_games": {
                    int pos = st.position(player.getUniqueId(), "games");
                    return pos == 0 ? none : String.valueOf(pos);
                }
                case "pos_kills": {
                    int pos = st.position(player.getUniqueId(), "kills");
                    return pos == 0 ? none : String.valueOf(pos);
                }
                default:
                    return null;
            }
        }
        return null;
    }
}
