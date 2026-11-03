package com.linktbl.oleadas;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnPointManager {

    public record Point(String world, double x, double y, double z) {
        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z);
        }

        @Override
        public String toString() {
            return world + ";" + x + ";" + y + ";" + z;
        }
    }

    private final OleadasPlugin plugin;
    private final File file;
    private final List<Point> points = new ArrayList<>();

    public SpawnPointManager(OleadasPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawnpoints.yml");
        load();
    }

    public synchronized void load() {
        points.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String s : y.getStringList("points")) {
            String[] a = s.split(";");
            if (a.length < 4) continue;
            try {
                points.add(new Point(a[0], Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public synchronized void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        List<String> out = new ArrayList<>();
        for (Point p : points) out.add(p.toString());
        y.set("points", out);
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar spawnpoints.yml: " + e.getMessage());
        }
    }

    public synchronized int add(Location l) {
        points.add(new Point(l.getWorld().getName(), l.getX(), l.getY(), l.getZ()));
        save();
        return points.size();
    }

    /** Borra el punto numero index1 (empezando en 1). */
    public synchronized boolean remove(int index1) {
        if (index1 < 1 || index1 > points.size()) return false;
        points.remove(index1 - 1);
        save();
        return true;
    }

    /** Borra los puntos a menos de 'radius' bloques de l. Devuelve cuantos borro. */
    public synchronized int removeNear(Location l, double radius) {
        int removed = 0;
        for (int i = points.size() - 1; i >= 0; i--) {
            Point p = points.get(i);
            if (!p.world().equals(l.getWorld().getName())) continue;
            double dx = p.x() - l.getX(), dy = p.y() - l.getY(), dz = p.z() - l.getZ();
            if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                points.remove(i);
                removed++;
            }
        }
        if (removed > 0) save();
        return removed;
    }

    public synchronized void clear() {
        points.clear();
        save();
    }

    public synchronized List<Point> all() {
        return new ArrayList<>(points);
    }

    public synchronized int size() {
        return points.size();
    }

    /** Un punto aleatorio (o null si no hay ninguno valido). */
    public synchronized Location random() {
        List<Location> valid = new ArrayList<>();
        for (Point p : points) {
            Location l = p.toLocation();
            if (l != null) valid.add(l);
        }
        if (valid.isEmpty()) return null;
        return valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
    }

    /** Mundos donde hay puntos de spawn (la "arena"). */
    public synchronized Set<World> worlds() {
        Set<World> set = new HashSet<>();
        for (Point p : points) {
            World w = Bukkit.getWorld(p.world());
            if (w != null) set.add(w);
        }
        return set;
    }
}
