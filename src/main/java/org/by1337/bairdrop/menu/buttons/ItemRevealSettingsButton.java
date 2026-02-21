package org.by1337.bairdrop.menu.buttons;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.CAirDrop;
import org.by1337.bairdrop.listeners.util.ListenChat;
import org.by1337.bairdrop.util.Message;

import java.util.*;

public class ItemRevealSettingsButton {

    public static final String TAG = "item_reveal_settings";
    public static final int SLOT = 43;

    private static final Map<UUID, Integer> playerSelectedIndex = new HashMap<>();

    public enum SettingType {
        ENABLED("Включено", true),
        ITEMS_PER_STEP("items-per-step", false),
        INTERVAL("interval", false),
        SOUND_ENABLED("sound enabled", true),
        SOUND("sound", false),
        VOLUME("volume", false),
        PITCH("pitch", false),
        RADIUS("radius", false);

        private final String displayName;
        private final boolean cyclic;

        SettingType(String displayName, boolean cyclic) {
            this.displayName = displayName;
            this.cyclic = cyclic;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isCyclic() {
            return cyclic;
        }
    }

    public static void resetPlayerIndex(UUID playerUUID) {
        playerSelectedIndex.put(playerUUID, 0);
    }

    public static ItemStack createItem(AirDrop airDrop, UUID playerUUID) {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(Message.messageBuilder("&eПлавное появление предметов"));

        int selectedIndex = (playerUUID != null) ? playerSelectedIndex.getOrDefault(playerUUID, 0) : 0;
        List<String> lore = buildLore(airDrop, selectedIndex);
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        meta.getPersistentDataContainer().set(NamespacedKey.fromString("tag"), PersistentDataType.STRING, TAG);
        item.setItemMeta(meta);

        return item;
    }

    private static List<String> buildLore(AirDrop airDrop, int selectedIndex) {
        List<String> lore = new ArrayList<>();

        if (!(airDrop instanceof CAirDrop cAirDrop)) {
            return lore;
        }

        lore.add("");

        SettingType[] settings = SettingType.values();

        for (int i = 0; i < settings.length; i++) {
            SettingType setting = settings[i];
            String prefix = (i == selectedIndex) ? " &9■ &n" : " &8□ &7";
            String value = getSettingValue(cAirDrop, setting);
            String coloredValue = getColoredValue(value);

            lore.add(Message.messageBuilder(prefix + setting.getDisplayName() + "&7: " + coloredValue));
        }

        lore.add("");
        lore.add(Message.messageBuilder("&a▶ ЛКМ &8- &7Вниз"));
        lore.add(Message.messageBuilder("&e▶ ПКМ &8- &7Вверх"));
        lore.add(Message.messageBuilder("&b▶ СКМ &8- &7Изменить"));

        return lore;
    }

    private static String getSettingValue(CAirDrop airDrop, SettingType setting) {
        return switch (setting) {
            case ENABLED -> String.valueOf(airDrop.isItemRevealEnabled());
            case ITEMS_PER_STEP -> airDrop.getItemRevealMinPerStep() + "-" + airDrop.getItemRevealMaxPerStep();
            case INTERVAL -> String.valueOf(airDrop.getItemRevealInterval());
            case SOUND_ENABLED -> String.valueOf(airDrop.isItemRevealStepSoundEnabled());
            case SOUND -> airDrop.getItemRevealStepSound();
            case VOLUME -> String.valueOf(airDrop.getItemRevealSoundVolume());
            case PITCH -> airDrop.getItemRevealSoundPitchMin() + "-" + airDrop.getItemRevealSoundPitchMax();
            case RADIUS -> String.valueOf(airDrop.getItemRevealSoundRadius());
        };
    }

    private static String getColoredValue(String value) {
        if (value.equalsIgnoreCase("true")) {
            return "&atrue";
        } else if (value.equalsIgnoreCase("false")) {
            return "&cfalse";
        }
        return "&6" + value;
    }

    public static void handleClick(AirDrop airDrop, Player player, ClickType clickType) {
        UUID uuid = player.getUniqueId();
        int currentIndex = playerSelectedIndex.getOrDefault(uuid, 0);
        SettingType[] settings = SettingType.values();

        if (clickType == ClickType.LEFT) {
            currentIndex = (currentIndex + 1) % settings.length;
            playerSelectedIndex.put(uuid, currentIndex);
        } else if (clickType == ClickType.RIGHT) {
            currentIndex = (currentIndex - 1 + settings.length) % settings.length;
            playerSelectedIndex.put(uuid, currentIndex);
        } else if (clickType == ClickType.MIDDLE) {
            SettingType selectedSetting = settings[currentIndex];
            handleSettingChange(airDrop, player, selectedSetting);
        }
    }

    private static void handleSettingChange(AirDrop airDrop, Player player, SettingType setting) {
        if (!(airDrop instanceof CAirDrop cAirDrop)) {
            return;
        }

        if (setting.isCyclic()) {
            cycleSetting(cAirDrop, setting);
            airDrop.save();
        } else {
            player.closeInventory();
            String chatKey = getChatKey(setting);
            ListenChat listenChat = new ListenChat(airDrop, chatKey, player);
            Bukkit.getServer().getPluginManager().registerEvents(listenChat, BAirDrop.getInstance());
        }
    }

    private static String getChatKey(SettingType setting) {
        return switch (setting) {
            case ITEMS_PER_STEP -> "item_reveal_items_per_step";
            case INTERVAL -> "item_reveal_interval";
            case SOUND -> "item_reveal_sound";
            case VOLUME -> "item_reveal_volume";
            case PITCH -> "item_reveal_pitch";
            case RADIUS -> "item_reveal_radius";
            default -> "";
        };
    }

    private static void cycleSetting(CAirDrop airDrop, SettingType setting) {
        switch (setting) {
            case ENABLED -> airDrop.setItemRevealEnabled(!airDrop.isItemRevealEnabled());
            case SOUND_ENABLED -> airDrop.setItemRevealStepSoundEnabled(!airDrop.isItemRevealStepSoundEnabled());
            default -> {}
        }
    }

    public static void clearPlayerData(UUID uuid) {
        playerSelectedIndex.remove(uuid);
    }
}
