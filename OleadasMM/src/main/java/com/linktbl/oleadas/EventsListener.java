package com.linktbl.oleadas;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EventsListener implements Listener {

    private final OleadasPlugin plugin;

    public EventsListener(OleadasPlugin plugin) {
        this.plugin = plugin;
    }

    /** Muestra particulas en los puntos de spawn a quien lleve la herramienta. */
    public void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!ToolItem.isTool(plugin, p.getInventory().getItemInMainHand())) continue;
                for (SpawnPointManager.Point pt : plugin.spawns().all()) {
                    Location l = pt.toLocation();
                    if (l == null || !l.getWorld().equals(p.getWorld())) continue;
                    if (l.distanceSquared(p.getLocation()) > 40 * 40) continue;
                    p.spawnParticle(Particle.END_ROD, l, 3, 0.2, 0.6, 0.2, 0.0);
                }
            }
        }, 20L, 10L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;
        ItemStack item = e.getItem();
        if (!ToolItem.isTool(plugin, item)) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("oleadas.admin")) return;

        Location base = e.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int n = plugin.spawns().add(base);
            plugin.send(p, "spawn-added",
                    "n", String.valueOf(n),
                    "x", String.valueOf(base.getBlockX()),
                    "y", String.valueOf(base.getBlockY()),
                    "z", String.valueOf(base.getBlockZ()));
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            int removed = plugin.spawns().removeNear(base, 1.6);
            if (removed > 0) plugin.send(p, "spawn-removed", "count", String.valueOf(removed));
            else plugin.send(p, "spawn-none-near");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (ToolItem.isTool(plugin, e.getPlayer().getInventory().getItemInMainHand())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent e) {
        if (e.getKiller() instanceof Player p) {
            plugin.waves().registerKill(p);
        }
    }
}
