package com.linktbl.oleadas;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    public static class PlayerStats {
        public final UUID id;
        public String name;
        public int highestWave;
        public int games;
        public int kills;

        public PlayerStats(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final OleadasPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> map = new HashMap<>();
    private final Map<String, List<PlayerStats>> cache = new HashMap<>();
    private long cacheAt = 0;
    private boolean dirty = false;

    public StatsManager(OleadasPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                PlayerStats s = new PlayerStats(id, sec.getString(k + ".name", "?"));
                s.highestWave = sec.getInt(k + ".wave");
                s.games = sec.getInt(k + ".games");
                s.kills = sec.getInt(k + ".kills");
                map.put(id, s);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (PlayerStats s : map.values()) {
            String b = "players." + s.id;
            y.set(b + ".name", s.name);
            y.set(b + ".wave", s.highestWave);
            y.set(b + ".games", s.games);
            y.set(b + ".kills", s.kills);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar stats.yml: " + e.getMessage());
        }
    }

    public synchronized void saveIfDirty() {
        if (dirty) save();
    }

    public synchronized void markDirty() {
        dirty = true;
        cacheAt = 0;
    }

    public synchronized PlayerStats get(UUID id) {
        return map.get(id);
    }

    public synchronized PlayerStats getOrCreate(UUID id, String name) {
        PlayerStats s = map.computeIfAbsent(id, k -> new PlayerStats(k, name));
        if (name != null) s.name = name;
        return s;
    }

    public static int value(PlayerStats s, String cat) {
        switch (cat) {
            case "wave": return s.highestWave;
            case "kills": return s.kills;
            case "games": return s.games;
            default: return 0;
        }
    }

    /** Ranking ordenado de mayor a menor (solo jugadores con valor > 0). */
    public synchronized List<PlayerStats> top(String cat) {
        long now = System.currentTimeMillis();
        if (now - cacheAt > 3000) {
            cache.clear();
            cacheAt = now;
        }
        return cache.computeIfAbsent(cat, c -> {
            List<PlayerStats> l = new ArrayList<>();
            for (PlayerStats s : map.values()) {
                if (value(s, c) > 0) l.add(s);
            }
            l.sort((a, b) -> {
                int d = Integer.compare(value(b, c), value(a, c));
                return d != 0 ? d : a.name.compareToIgnoreCase(b.name);
            });
            return l;
        });
    }

    /** Posicion (1 = primero) del jugador en esa categoria, o 0 si no esta en el ranking. */
    public synchronized int position(UUID id, String cat) {
        List<PlayerStats> l = top(cat);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(id)) return i + 1;
        }
        return 0;
    }
}
