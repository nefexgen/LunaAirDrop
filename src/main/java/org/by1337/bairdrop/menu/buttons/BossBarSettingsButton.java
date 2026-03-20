package org.by1337.bairdrop.menu.buttons;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.bossbar.AirDropBossBar;
import org.by1337.bairdrop.listeners.util.ListenChat;
import org.by1337.bairdrop.util.Message;

import java.util.*;

public class BossBarSettingsButton {

    public static final String TAG = "bossbar_settings";
    public static final int SLOT = 53;
    
    private static final Map<UUID, Integer> playerSelectedIndex = new HashMap<>();
    
    private static final String[] VISIBILITY_VALUES = {"radius", "global"};
    private static final BarColor[] COLOR_VALUES = BarColor.values();
    private static final BarStyle[] STYLE_VALUES = BarStyle.values();

    public enum SettingType {
        ENABLED("Включен", true),
        VISIBILITY("visibility", true),
        RADIUS("radius", false),
        COLOR("color", true),
        STYLE("style", true);

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
        ItemStack item = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(Message.messageBuilder("&dBossBar"));
        
        int selectedIndex = (playerUUID != null) ? playerSelectedIndex.getOrDefault(playerUUID, 0) : 0;
        List<String> lore = buildLore(airDrop, selectedIndex);
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(NamespacedKey.fromString("tag"), PersistentDataType.STRING, TAG);
        item.setItemMeta(meta);
        
        return item;
    }

    private static List<String> buildLore(AirDrop airDrop, int selectedIndex) {
        List<String> lore = new ArrayList<>();
        AirDropBossBar bossBar = airDrop.getAirDropBossBar();
        
        lore.add("");
        
        SettingType[] settings = SettingType.values();
        
        for (int i = 0; i < settings.length; i++) {
            SettingType setting = settings[i];
            String prefix = (i == selectedIndex) ? " &9■ &n" : " &8□ &7";
            String value = getSettingValue(bossBar, setting);
            String coloredValue = getColoredValue(setting, value);
            
            lore.add(Message.messageBuilder(prefix + setting.getDisplayName() + "&7: " + coloredValue));
        }
        
        lore.add("");
        lore.add(Message.messageBuilder("&a▶ ЛКМ &8- &7Вниз"));
        lore.add(Message.messageBuilder("&e▶ ПКМ &8- &7Вверх"));
        lore.add(Message.messageBuilder("&b▶ СКМ &8- &7Изменить"));
        
        return lore;
    }

    private static String getSettingValue(AirDropBossBar bossBar, SettingType setting) {
        return switch (setting) {
            case ENABLED -> String.valueOf(bossBar.isEnabled());
            case VISIBILITY -> bossBar.getVisibility();
            case RADIUS -> String.valueOf(bossBar.getRadius());
            case COLOR -> bossBar.getColor().name();
            case STYLE -> bossBar.getStyle().name();
        };
    }

    private static String getColoredValue(SettingType setting, String value) {
        return switch (setting) {
            case ENABLED -> value.equals("true") ? "&atrue" : "&cfalse";
            case VISIBILITY -> "&6" + value;
            case RADIUS -> "&6" + value;
            case COLOR -> "&6" + value;
            case STYLE -> "&6" + value;
        };
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
        AirDropBossBar bossBar = airDrop.getAirDropBossBar();
        
        if (setting.isCyclic()) {
            cycleSetting(bossBar, setting);
            airDrop.save();
        } else {
            player.closeInventory();
            ListenChat listenChat = new ListenChat(airDrop, "bossbar_radius", player);
            Bukkit.getServer().getPluginManager().registerEvents(listenChat, BAirDrop.getInstance());
        }
    }

    private static void cycleSetting(AirDropBossBar bossBar, SettingType setting) {
        if (setting == SettingType.ENABLED) {
            bossBar.setEnabled(!bossBar.isEnabled());
        } else if (setting == SettingType.VISIBILITY) {
            String current = bossBar.getVisibility();
            int index = 0;
            for (int i = 0; i < VISIBILITY_VALUES.length; i++) {
                if (VISIBILITY_VALUES[i].equalsIgnoreCase(current)) {
                    index = i;
                    break;
                }
            }
            bossBar.setVisibility(VISIBILITY_VALUES[(index + 1) % VISIBILITY_VALUES.length]);
        } else if (setting == SettingType.COLOR) {
            BarColor current = bossBar.getColor();
            int index = 0;
            for (int i = 0; i < COLOR_VALUES.length; i++) {
                if (COLOR_VALUES[i] == current) {
                    index = i;
                    break;
                }
            }
            bossBar.setColor(COLOR_VALUES[(index + 1) % COLOR_VALUES.length]);
        } else if (setting == SettingType.STYLE) {
            BarStyle current = bossBar.getStyle();
            int index = 0;
            for (int i = 0; i < STYLE_VALUES.length; i++) {
                if (STYLE_VALUES[i] == current) {
                    index = i;
                    break;
                }
            }
            bossBar.setStyle(STYLE_VALUES[(index + 1) % STYLE_VALUES.length]);
        }
    }

    public static void clearPlayerData(UUID uuid) {
        playerSelectedIndex.remove(uuid);
    }
}
