package org.by1337.bairdrop.menu.buttons;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.util.Message;

import java.util.*;

public class BlockFacingButton {

    public static final String TAG = "block_facing_settings";
    public static final int SLOT = 45;
    
    private static final String[] FACING_VALUES = {"NONE", "NORTH", "SOUTH", "EAST", "WEST"};

    public static ItemStack createItem(AirDrop airDrop) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(Message.messageBuilder("&aНаправление блока"));
        
        List<String> lore = buildLore(airDrop);
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(NamespacedKey.fromString("tag"), PersistentDataType.STRING, TAG);
        item.setItemMeta(meta);
        
        return item;
    }

    private static List<String> buildLore(AirDrop airDrop) {
        List<String> lore = new ArrayList<>();
        
        lore.add("");
        lore.add(Message.messageBuilder(" &9■ &nblock-facing&7: &6" + airDrop.getChestFacing()));
        lore.add("");
        lore.add(Message.messageBuilder("&b▶ СКМ &8- &7Изменить"));
        
        return lore;
    }

    public static void handleClick(AirDrop airDrop, Player player, ClickType clickType) {
        if (clickType == ClickType.MIDDLE) {
            cycleFacing(airDrop);
            airDrop.save();
        }
    }

    private static void cycleFacing(AirDrop airDrop) {
        String current = airDrop.getChestFacing();
        int index = 0;
        for (int i = 0; i < FACING_VALUES.length; i++) {
            if (FACING_VALUES[i].equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        airDrop.setChestFacing(FACING_VALUES[(index + 1) % FACING_VALUES.length]);
    }
}
