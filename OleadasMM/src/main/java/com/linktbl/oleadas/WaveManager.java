package com.linktbl.oleadas;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WaveManager {

    private final OleadasPlugin plugin;

    private boolean running = false;
    private int currentWave = 0;
    private int spawnedInWave = 0;
    private int waveAmount = 0;
    private long nextWaveAt = 0; // millis, 0 = aun no programada
    private int cleanCounter = 0;
    private volatile int aliveCache = 0;

    private BukkitTask startTask, spawnTask, nextTask, mainTask;
    private final Set<UUID> participants = new HashSet<>();
    private final Set<String> warned = new HashSet<>();

    public WaveManager(OleadasPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    // ---------------------------------------------------------------- control

    public boolean start(CommandSender sender) {
        if (running) {
            plugin.send(sender, "already-running");
            return false;
        }
        if (plugin.spawns().size() == 0) {
            plugin.send(sender, "no-spawnpoints");
            return false;
        }
        if (cfg().getConfigurationSection("waves.1") == null) {
            plugin.send(sender, "no-wave-1");
            return false;
        }

        running = true;
        participants.clear();
        warned.clear();
        currentWave = 0;
        spawnedInWave = 0;
        waveAmount = 0;
        nextWaveAt = 0;
        cleanCounter = 0;

        int delay = Math.max(0, cfg().getInt("settings.start-delay-seconds", 5));
        broadcast(plugin.msg("game-start", "seconds", String.valueOf(delay)));

        startTask = Bukkit.getScheduler().runTaskLater(plugin, () -> startWave(1), delay * 20L);
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        return true;
    }

    /** Finaliza la partida. natural = true cuando se acaban las oleadas. */
    public void end(boolean natural) {
        if (!running) return;
        int last = currentWave;
        running = false;
        cancelAll();
        killAll();
        plugin.stats().save();
        currentWave = 0;
        spawnedInWave = 0;
        waveAmount = 0;
        nextWaveAt = 0;
        aliveCache = 0;
        participants.clear();
        broadcast(plugin.msg(natural ? "game-finished" : "game-forced-end", "wave", String.valueOf(last)));
    }

    public boolean setWave(CommandSender sender, int n) {
        if (!running) {
            plugin.send(sender, "not-running");
            return false;
        }
        if (cfg().getConfigurationSection("waves." + n) == null) {
            plugin.send(sender, "wave-not-found", "wave", String.valueOf(n));
            return false;
        }
        if (startTask != null) startTask.cancel();
        plugin.send(sender, "wave-set", "wave", String.valueOf(n));
        startWave(n);
        return true;
    }

    /** Se llama al apagar el plugin/servidor. */
    public void shutdown() {
        try {
            if (running) {
                running = false;
                cancelAll();
                killAll();
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ waves

    private void startWave(int n) {
        if (!running) return;
        ConfigurationSection sec = cfg().getConfigurationSection("waves." + n);
        if (sec == null) {
            end(true); // ya no hay mas oleadas
            return;
        }
        if (spawnTask != null) spawnTask.cancel();
        if (nextTask != null) nextTask.cancel();

        currentWave = n;
        spawnedInWave = 0;
        waveAmount = Math.max(0, sec.getInt("amount", 10));
        nextWaveAt = 0;

        final List<String> pool = sec.getStringList("mobs");
        double delay = sec.getDouble("delay", 1.0);
        long period = Math.max(1L, Math.round(delay * 20.0));
        final double nextSeconds = sec.getDouble("nextwave", 60.0);

        // Registrar a los jugadores de la arena (partidas jugadas / wave mas alta)
        for (Player p : arenaPlayers()) touch(p);

        broadcast(plugin.msg("wave-start", "wave", String.valueOf(n), "amount", String.valueOf(waveAmount)));

        if (waveAmount == 0 || pool.isEmpty()) {
            if (pool.isEmpty()) plugin.getLogger().warning("La oleada " + n + " no tiene mobs en 'mobs:'.");
            scheduleNext(n, nextSeconds);
            return;
        }

        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                Location loc = plugin.spawns().random();
                if (loc != null) spawnOne(pool, loc);
                spawnedInWave++;
                if (spawnedInWave >= waveAmount) {
                    cancel();
                    scheduleNext(n, nextSeconds);
                }
            }
        }.runTaskTimer(plugin, 0L, period);
    }

    private void scheduleNext(int n, double seconds) {
        long ticks = Math.max(1L, Math.round(seconds * 20.0));
        nextWaveAt = System.currentTimeMillis() + Math.round(seconds * 1000.0);
        nextTask = Bukkit.getScheduler().runTaskLater(plugin, () -> startWave(n + 1), ticks);
    }

    private void spawnOne(List<String> pool, Location loc) {
        String type = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        try {
            ActiveMob m = MythicBukkit.inst().getMobManager().spawnMob(type, loc);
            if (m == null && warned.add(type)) {
                plugin.getLogger().warning("No se pudo spawnear '" + type + "'. ¿Existe ese mob en MythicMobs?");
            }
        } catch (Throwable t) {
            if (warned.add(type)) {
                plugin.getLogger().warning("Error al spawnear '" + type + "': " + t.getMessage());
            }
        }
    }

    // ------------------------------------------------------- tick / cleaning

    private void tick() {
        if (!running) return;
        aliveCache = activeMobs(true).size();

        if (!cfg().getBoolean("settings.clean.enabled", true)) return;
        cleanCounter++;
        int interval = Math.max(1, cfg().getInt("settings.clean.check-interval-seconds", 10));
        if (cleanCounter >= interval) {
            cleanCounter = 0;
            clean();
        }
    }

    private void clean() {
        List<ActiveMob> list = activeMobs(false);
        int max = cfg().getInt("settings.clean.max-mobs", 50);
        int keep = Math.max(0, cfg().getInt("settings.clean.keep-mobs", 10));
        if (list.size() <= max) return;

        int toRemove = list.size() - keep;
        if (toRemove <= 0) return;

        final List<Player> players = arenaPlayers();
        // Primero se eliminan los mas lejanos a los jugadores
        list.sort((a, b) -> Double.compare(nearest(bukkit(b), players), nearest(bukkit(a), players)));

        int removed = 0;
        for (ActiveMob am : list) {
            if (removed >= toRemove) break;
            Entity e = bukkit(am);
            if (e != null) {
                e.remove();
                removed++;
            }
        }
        plugin.getLogger().info("Limpieza: " + removed + " mobs eliminados (habia " + list.size() + ").");
    }

    private double nearest(Entity e, List<Player> players) {
        double best = Double.MAX_VALUE;
        for (Player p : players) {
            if (p.getWorld().equals(e.getWorld())) {
                best = Math.min(best, p.getLocation().distanceSquared(e.getLocation()));
            }
        }
        return best == Double.MAX_VALUE ? 0 : best;
    }

    private Entity bukkit(ActiveMob am) {
        return am.getEntity() == null ? null : am.getEntity().getBukkitEntity();
    }

    /** Mobs de MythicMobs vivos dentro de la arena. */
    private List<ActiveMob> activeMobs(boolean includeBlacklisted) {
        List<ActiveMob> out = new ArrayList<>();
        Set<World> worlds = plugin.spawns().worlds();
        Set<String> blacklist = new HashSet<>();
        for (String s : cfg().getStringList("settings.clean.blacklist")) blacklist.add(s.toLowerCase());

        try {
            for (ActiveMob am : new ArrayList<>(MythicBukkit.inst().getMobManager().getActiveMobs())) {
                if (am.isDead()) continue;
                Entity e = bukkit(am);
                if (e == null || !e.isValid()) continue;
                if (!worlds.contains(e.getWorld())) continue;
                if (!includeBlacklisted && blacklist.contains(am.getMobType().toLowerCase())) continue;
                out.add(am);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Error leyendo mobs activos de MythicMobs: " + t.getMessage());
        }
        return out;
    }

    private void killAll() {
        for (ActiveMob am : activeMobs(true)) {
            Entity e = bukkit(am);
            if (e != null) e.remove();
        }
    }

    private void cancelAll() {
        if (startTask != null) startTask.cancel();
        if (spawnTask != null) spawnTask.cancel();
        if (nextTask != null) nextTask.cancel();
        if (mainTask != null) mainTask.cancel();
        startTask = spawnTask = nextTask = mainTask = null;
    }

    // ------------------------------------------------------------ jugadores

    private List<Player> arenaPlayers() {
        List<Player> list = new ArrayList<>();
        Set<World> worlds = plugin.spawns().worlds();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worlds.contains(p.getWorld())) list.add(p);
        }
        return list;
    }

    private void touch(Player p) {
        StatsManager.PlayerStats s = plugin.stats().getOrCreate(p.getUniqueId(), p.getName());
        if (participants.add(p.getUniqueId())) s.games++;
        if (currentWave > s.highestWave) s.highestWave = currentWave;
        plugin.stats().markDirty();
    }

    /** Un jugador mato un mob de MythicMobs. */
    public void registerKill(Player p) {
        if (!running) return;
        touch(p);
        plugin.stats().getOrCreate(p.getUniqueId(), p.getName()).kills++;
        plugin.stats().markDirty();
    }

    private void broadcast(String text) {
        Set<World> worlds = plugin.spawns().worlds();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worlds.contains(p.getWorld())) p.sendMessage(text);
        }
        Bukkit.getConsoleSender().sendMessage(text);
    }

    // --------------------------------------------------------------- getters

    public boolean isRunning() { return running; }
    public int getCurrentWave() { return currentWave; }
    public int getSpawnedInWave() { return spawnedInWave; }
    public int getWaveAmount() { return waveAmount; }
    public int getAlive() { return aliveCache; }
    public int getToSpawn() { return Math.max(0, waveAmount - spawnedInWave); }

    public int getSecondsToNext() {
        if (!running || nextWaveAt == 0) return 0;
        return (int) Math.max(0, (nextWaveAt - System.currentTimeMillis() + 999) / 1000);
    }

    public int getTotalWaves() {
        ConfigurationSection s = cfg().getConfigurationSection("waves");
        if (s == null) return 0;
        int max = 0;
        for (String k : s.getKeys(false)) {
            try {
                max = Math.max(max, Integer.parseInt(k));
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }
}
