package com.linktbl.oleadas;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WaveCommand implements CommandExecutor, TabCompleter {

    private final OleadasPlugin plugin;

    public WaveCommand(OleadasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("oleadas.admin")) {
            plugin.send(s, "no-permission");
            return true;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "start": {
                plugin.waves().start(s);
                break;
            }
            case "forceend":
            case "end":
            case "stop": {
                if (!plugin.waves().isRunning()) plugin.send(s, "not-running");
                else plugin.waves().end(false);
                break;
            }
            case "set": {
                if (a.length < 2) {
                    s.sendMessage(plugin.color("&cUso: /wave set <numero>"));
                    break;
                }
                try {
                    plugin.waves().setWave(s, Integer.parseInt(a[1]));
                } catch (NumberFormatException ex) {
                    s.sendMessage(plugin.color("&cEl numero de oleada no es valido."));
                }
                break;
            }
            case "tool": {
                if (!(s instanceof Player)) {
                    plugin.send(s, "players-only");
                    break;
                }
                Player p = (Player) s;
                p.getInventory().addItem(ToolItem.create(plugin));
                plugin.send(p, "tool-given");
                break;
            }
            case "spawns": {
                spawns(s, a);
                break;
            }
            case "reload": {
                plugin.reloadConfig();
                plugin.spawns().load();
                plugin.send(s, "reloaded");
                break;
            }
            case "status": {
                WaveManager w = plugin.waves();
                s.sendMessage(plugin.color("&6Partida: " + (w.isRunning() ? "&aen curso" : "&cdetenida")));
                s.sendMessage(plugin.color("&6Oleada: &f" + w.getCurrentWave() + "&7/" + w.getTotalWaves()));
                s.sendMessage(plugin.color("&6Spawneados: &f" + w.getSpawnedInWave() + "&7/" + w.getWaveAmount()));
                s.sendMessage(plugin.color("&6Mobs vivos: &f" + w.getAlive()));
                s.sendMessage(plugin.color("&6Siguiente oleada en: &f" + w.getSecondsToNext() + "s"));
                s.sendMessage(plugin.color("&6Puntos de spawn: &f" + plugin.spawns().size()));
                break;
            }
            default:
                help(s);
        }
        return true;
    }

    private void spawns(CommandSender s, String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "list": {
                List<SpawnPointManager.Point> pts = plugin.spawns().all();
                s.sendMessage(plugin.color("&6Puntos de spawn (" + pts.size() + "):"));
                for (int i = 0; i < pts.size(); i++) {
                    SpawnPointManager.Point p = pts.get(i);
                    s.sendMessage(plugin.color("&e#" + (i + 1) + " &f" + p.world() + " "
                            + (int) Math.floor(p.x()) + ", " + (int) Math.floor(p.y()) + ", " + (int) Math.floor(p.z())));
                }
                break;
            }
            case "remove": {
                if (a.length < 3) {
                    s.sendMessage(plugin.color("&cUso: /wave spawns remove <numero>"));
                    break;
                }
                try {
                    if (plugin.spawns().remove(Integer.parseInt(a[2]))) plugin.send(s, "spawn-removed", "count", "1");
                    else plugin.send(s, "spawn-invalid");
                } catch (NumberFormatException ex) {
                    plugin.send(s, "spawn-invalid");
                }
                break;
            }
            case "clear": {
                plugin.spawns().clear();
                plugin.send(s, "spawn-cleared");
                break;
            }
            case "tp": {
                if (!(s instanceof Player) || a.length < 3) {
                    s.sendMessage(plugin.color("&cUso (jugador): /wave spawns tp <numero>"));
                    break;
                }
                try {
                    int i = Integer.parseInt(a[2]);
                    List<SpawnPointManager.Point> pts = plugin.spawns().all();
                    if (i < 1 || i > pts.size()) {
                        plugin.send(s, "spawn-invalid");
                        break;
                    }
                    Location l = pts.get(i - 1).toLocation();
                    if (l == null) {
                        plugin.send(s, "spawn-invalid");
                        break;
                    }
                    ((Player) s).teleport(l);
                } catch (NumberFormatException ex) {
                    plugin.send(s, "spawn-invalid");
                }
                break;
            }
            default:
                s.sendMessage(plugin.color("&cUso: /wave spawns <list|remove|clear|tp>"));
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(plugin.color("&8&m----------&r &c&lOleadas &8&m----------"));
        s.sendMessage(plugin.color("&e/wave start &7- Inicia la partida"));
        s.sendMessage(plugin.color("&e/wave forceend &7- Termina la partida y borra los mobs"));
        s.sendMessage(plugin.color("&e/wave set <n> &7- Salta a la oleada n"));
        s.sendMessage(plugin.color("&e/wave tool &7- Herramienta para marcar puntos de spawn"));
        s.sendMessage(plugin.color("&e/wave spawns <list|remove n|clear|tp n>"));
        s.sendMessage(plugin.color("&e/wave status &7- Estado de la partida"));
        s.sendMessage(plugin.color("&e/wave reload &7- Recarga config y puntos"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("oleadas.admin")) return out;
        if (a.length == 1) {
            for (String x : Arrays.asList("start", "forceend", "set", "tool", "spawns", "status", "reload")) {
                if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
            }
        } else if (a.length == 2 && a[0].equalsIgnoreCase("spawns")) {
            for (String x : Arrays.asList("list", "remove", "clear", "tp")) {
                if (x.startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(x);
            }
        }
        return out;
    }
}
