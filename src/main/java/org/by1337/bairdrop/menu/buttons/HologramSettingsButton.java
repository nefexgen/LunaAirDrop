package org.by1337.bairdrop.menu.buttons;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.hologram.HologramSettings;
import org.by1337.bairdrop.hologram.HologramType;
import org.by1337.bairdrop.listeners.util.ListenChat;
import org.by1337.bairdrop.util.Message;

import java.util.*;

public class HologramSettingsButton {

    public static final String TAG = "hologram_settings";
    public static final int SLOT = 37;
    
    private static final Map<UUID, Integer> playerSelectedIndex = new HashMap<>();
    
    private static final HologramType[] HOLOGRAM_TYPES = HologramType.values();
    private static final Display.Billboard[] BILLBOARDS = Display.Billboard.values();
    private static final TextDisplay.TextAlignment[] ALIGNMENTS = TextDisplay.TextAlignment.values();

    public enum SettingType {
        HOLOGRAMS("holograms", true, null),
        HOLO_OFFSET_Y("holo-offset Y", false, "holo_offset_y"),
        TEXT_SHADOW("text-shadow", true, null),
        TEXT_OPACITY("text-opacity", false, "holo_text_opacity"),
        BACKGROUND_COLOR("background-color", false, "holo_bg_color"),
        BACKGROUND_OPACITY("background-opacity", false, "holo_bg_opacity"),
        SEE_THROUGH("see-through", true, null),
        VIEW_RANGE("view-range", false, "holo_view_range"),
        BRIGHTNESS("brightness", false, "holo_brightness"),
        SCALE("scale", false, "holo_scale"),
        TEXT_ALIGNMENT("text-alignment", true, null),
        BILLBOARD("billboard", true, null),
        YAW("yaw", false, "holo_yaw"),
        PITCH("pitch", false, "holo_pitch");

        private final String displayName;
        private final boolean cyclic;
        private final String chatKey;

        SettingType(String displayName, boolean cyclic, String chatKey) {
            this.displayName = displayName;
            this.cyclic = cyclic;
            this.chatKey = chatKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isCyclic() {
            return cyclic;
        }

        public String getChatKey() {
            return chatKey;
        }
    }

    public static void resetPlayerIndex(UUID playerUUID) {
        playerSelectedIndex.put(playerUUID, 0);
    }

    public static ItemStack createItem(AirDrop airDrop, UUID playerUUID) {
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(Message.messageBuilder("&bГолограммы"));
        
        int selectedIndex = (playerUUID != null) ? playerSelectedIndex.getOrDefault(playerUUID, 0) : 0;
        List<String> lore = buildLore(airDrop, selectedIndex);
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(NamespacedKey.fromString("tag"), PersistentDataType.STRING, TAG);
        item.setItemMeta(meta);
        
        return item;
    }

    private static List<String> buildLore(AirDrop airDrop, int selectedIndex) {
        List<String> lore = new ArrayList<>();
        
        lore.add("");
        
        SettingType[] settings = SettingType.values();
        
        for (int i = 0; i < settings.length; i++) {
            SettingType setting = settings[i];
            String prefix = (i == selectedIndex) ? " &9■ &n" : " &8□ &7";
            String value = getSettingValue(airDrop, setting);
            String coloredValue = getColoredValue(setting, value);
            
            lore.add(Message.messageBuilder(prefix + setting.getDisplayName() + "&7: " + coloredValue));
        }
        
        lore.add("");
        lore.add(Message.messageBuilder("&a▶ ЛКМ &8- &7Вниз"));
        lore.add(Message.messageBuilder("&e▶ ПКМ &8- &7Вверх"));
        lore.add(Message.messageBuilder("&b▶ СКМ &8- &7Изменить"));
        
        return lore;
    }

    private static String getSettingValue(AirDrop airDrop, SettingType setting) {
        HologramSettings hs = airDrop.getHologramSettings();
        return switch (setting) {
            case HOLOGRAMS -> airDrop.getHologramType() != null ? airDrop.getHologramType().name().toLowerCase() : "null";
            case HOLO_OFFSET_Y -> String.valueOf(airDrop.getHoloOffsets().getY());
            case TEXT_SHADOW -> String.valueOf(hs.isTextShadow());
            case TEXT_OPACITY -> String.valueOf((hs.getTextOpacity() & 0xFF) * 100 / 255);
            case BACKGROUND_COLOR -> formatColor(hs.getBackgroundColor());
            case BACKGROUND_OPACITY -> String.valueOf(hs.getBackgroundOpacity());
            case SEE_THROUGH -> String.valueOf(hs.isSeeThrough());
            case VIEW_RANGE -> String.valueOf(hs.getViewRange());
            case BRIGHTNESS -> String.valueOf(hs.getBrightness());
            case SCALE -> String.valueOf(hs.getScale());
            case TEXT_ALIGNMENT -> hs.getTextAlignment().name();
            case BILLBOARD -> hs.getBillboard().name();
            case YAW -> String.valueOf(hs.getYaw());
            case PITCH -> String.valueOf(hs.getPitch());
        };
    }

    private static String formatColor(org.bukkit.Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String getColoredValue(SettingType setting, String value) {
        if (value.equalsIgnoreCase("true")) {
            return "&a" + value;
        } else if (value.equalsIgnoreCase("false")) {
            return "&c" + value;
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
        if (setting.isCyclic()) {
            cycleSetting(airDrop, setting);
            airDrop.save();
        } else {
            player.closeInventory();
            ListenChat listenChat = new ListenChat(airDrop, setting.getChatKey(), player);
            Bukkit.getServer().getPluginManager().registerEvents(listenChat, BAirDrop.getInstance());
        }
    }

    private static void cycleSetting(AirDrop airDrop, SettingType setting) {
        HologramSettings hs = airDrop.getHologramSettings();
        switch (setting) {
            case HOLOGRAMS -> {
                HologramType current = airDrop.getHologramType();
                int index = 0;
                if (current != null) {
                    for (int i = 0; i < HOLOGRAM_TYPES.length; i++) {
                        if (HOLOGRAM_TYPES[i] == current) {
                            index = i;
                            break;
                        }
                    }
                }
                airDrop.setHologramType(HOLOGRAM_TYPES[(index + 1) % HOLOGRAM_TYPES.length]);
            }
            case TEXT_SHADOW -> hs.setTextShadow(!hs.isTextShadow());
            case SEE_THROUGH -> hs.setSeeThrough(!hs.isSeeThrough());
            case TEXT_ALIGNMENT -> {
                int idx = 0;
                for (int i = 0; i < ALIGNMENTS.length; i++) {
                    if (ALIGNMENTS[i] == hs.getTextAlignment()) {
                        idx = i;
                        break;
                    }
                }
                hs.setTextAlignment(ALIGNMENTS[(idx + 1) % ALIGNMENTS.length]);
            }
            case BILLBOARD -> {
                int idx = 0;
                for (int i = 0; i < BILLBOARDS.length; i++) {
                    if (BILLBOARDS[i] == hs.getBillboard()) {
                        idx = i;
                        break;
                    }
                }
                hs.setBillboard(BILLBOARDS[(idx + 1) % BILLBOARDS.length]);
            }
            default -> {}
        }
    }

    public static void clearPlayerData(UUID uuid) {
        playerSelectedIndex.remove(uuid);
    }
}
