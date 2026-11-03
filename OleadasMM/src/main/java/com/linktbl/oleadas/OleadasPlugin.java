package com.linktbl.oleadas;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class OleadasPlugin extends JavaPlugin {

    private SpawnPointManager spawns;
    private StatsManager stats;
    private WaveManager waves;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        spawns = new SpawnPointManager(this);
        stats = new StatsManager(this);
        waves = new WaveManager(this);

        WaveCommand cmd = new WaveCommand(this);
        getCommand("wave").setExecutor(cmd);
        getCommand("wave").setTabCompleter(cmd);

        EventsListener listener = new EventsListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        listener.startParticleTask();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OleadasExpansion(this).register();
            getLogger().info("PlaceholderAPI detectado: placeholders %oleadas_...% registrados.");
        } else {
            getLogger().warning("PlaceholderAPI no esta instalado: los placeholders no estaran disponibles.");
        }

        // Guardado automatico de estadisticas cada 5 minutos
        Bukkit.getScheduler().runTaskTimer(this, () -> stats.saveIfDirty(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (waves != null) waves.shutdown();
        if (stats != null) stats.save();
        if (spawns != null) spawns.save();
    }

    public SpawnPointManager spawns() { return spawns; }
    public StatsManager stats() { return stats; }
    public WaveManager waves() { return waves; }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Mensaje de messages.<key> con prefijo y reemplazos {clave} -> valor. */
    public String msg(String key, String... kv) {
        String raw = getConfig().getString("messages." + key, "&cFalta messages." + key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return color(getConfig().getString("messages.prefix", "") + raw);
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(msg(key, kv));
    }
}
