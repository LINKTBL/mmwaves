package com.linktbl.oleadas;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ToolItem {

    private ToolItem() {
    }

    public static ItemStack create(OleadasPlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&c&lHerramienta de Spawns"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Click derecho en un bloque: &aañadir punto"));
        lore.add(plugin.color("&7Click izquierdo en un bloque: &cquitar punto"));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "spawn_tool"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTool(OleadasPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "spawn_tool"), PersistentDataType.BYTE);
    }
}
