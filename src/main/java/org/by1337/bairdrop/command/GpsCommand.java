package org.by1337.bairdrop.command;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.by1337.bairdrop.util.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GpsCommand implements CommandExecutor, TabCompleter {

    private static final Map<UUID, GpsSession> activeSessions = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Message.logger(BAirDrop.getConfigMessage().getMessage("only-player"));
            return true;
        }

        if (!player.hasPermission("bair.gps")) {
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("no-prem"));
            return true;
        }

        if (!BAirDrop.getInstance().getConfig().getBoolean("airdrop-navigator.enabled", true)) {
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-navigator-disabled"));
            return true;
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            stopGps(player);
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-disabled"));
            return true;
        }

        if (args.length > 0) {
            String airdropId = args[0];
            AirDrop targetAirdrop = BAirDrop.airDrops.get(airdropId);
            if (targetAirdrop != null && targetAirdrop.isAirDropStarted()) {
                Location loc = targetAirdrop.getAirDropLocation();
                if (loc != null && loc.getWorld() != null) {
                    startGps(player, loc.clone());
                    Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-enabled"));
                    return true;
                }
            }
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-no-airdrops"));
            return true;
        }

        List<AirDrop> activeAirdrops = getActiveAirdrops();
        
        if (activeAirdrops.isEmpty()) {
            Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-no-airdrops"));
            return true;
        }

        if (activeAirdrops.size() == 1) {
            AirDrop airdrop = activeAirdrops.get(0);
            Location loc = airdrop.getAirDropLocation();
            if (loc != null) {
                startGps(player, loc.clone());
                Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-enabled"));
            }
            return true;
        }

        showAirdropList(player, activeAirdrops);
        return true;
    }

    private List<AirDrop> getActiveAirdrops() {
        List<AirDrop> active = new ArrayList<>();
        for (AirDrop air : BAirDrop.airDrops.values()) {
            if (air.isAirDropStarted()) {
                Location loc = air.getAirDropLocation();
                if (loc != null && loc.getWorld() != null) {
                    active.add(air);
                }
            }
        }
        return active;
    }

    private void showAirdropList(Player player, List<AirDrop> airdrops) {
        Message.sendMsg(player, BAirDrop.getConfigMessage().getMessage("gps-select-airdrop"));
        
        String lineFormat = BAirDrop.getConfigMessage().getMessage("gps-list-line");
        String buttonText = BAirDrop.getConfigMessage().getMessage("gps-list-button");
        String buttonHover = BAirDrop.getConfigMessage().getMessage("gps-list-button-hover");
        String otherWorldText = BAirDrop.getConfigMessage().getMessage("gps-list-other-world");
        
        int index = 1;
        for (AirDrop air : airdrops) {
            Location loc = air.getAirDropLocation();
            if (loc == null) continue;
            
            double distance = player.getWorld().equals(loc.getWorld()) 
                ? player.getLocation().distance(loc) 
                : -1;
            
            String distanceText = distance >= 0 ? (int) distance + "m" : otherWorldText;
            
            String line = lineFormat
                .replace("{num}", String.valueOf(index))
                .replace("{name}", air.getEventListName())
                .replace("{distance}", distanceText);
            
            Component message = Message.messageBuilderComponent(line).append(
                Message.messageBuilderComponent(buttonText)
                    .clickEvent(ClickEvent.runCommand("/gps " + air.getId()))
                    .hoverEvent(HoverEvent.showText(Message.messageBuilderComponent(buttonHover)))
            );
            
            player.sendMessage(message);
            index++;
        }
    }

    private void startGps(Player player, Location targetLocation) {
        String messageFormat = BAirDrop.getInstance().getConfig().getString(
            "airdrop-navigator.bossbar-settings.message",
            "&7[&#00B7EC%distance%m&7] &fНаправление аир-дропа: &7[&#00B7EC%direction%&7]"
        );
        int interval = BAirDrop.getInstance().getConfig().getInt("airdrop-navigator.bossbar-settings.interval", 5);
        boolean roundedDistance = BAirDrop.getInstance().getConfig().getBoolean("airdrop-navigator.rounded-distance", false);
        boolean roundedScale = BAirDrop.getInstance().getConfig().getBoolean("airdrop-navigator.rounded-scale", false);
        boolean ignoreHeight = BAirDrop.getInstance().getConfig().getBoolean("airdrop-navigator.ignore-height", false);

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(BAirDrop.getInstance().getConfig().getString("airdrop-navigator.bossbar-settings.color", "BLUE").toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.BLUE;
        }

        BossBar.Overlay overlay;
        try {
            String styleStr = BAirDrop.getInstance().getConfig().getString("airdrop-navigator.bossbar-settings.style", "SOLID").toUpperCase();
            overlay = switch (styleStr) {
                case "SEGMENTED_6" -> BossBar.Overlay.NOTCHED_6;
                case "SEGMENTED_10" -> BossBar.Overlay.NOTCHED_10;
                case "SEGMENTED_12" -> BossBar.Overlay.NOTCHED_12;
                case "SEGMENTED_20" -> BossBar.Overlay.NOTCHED_20;
                default -> BossBar.Overlay.PROGRESS;
            };
        } catch (Exception e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        BossBar bossBar = BossBar.bossBar(Component.text(""), 0f, color, overlay);
        player.showBossBar(bossBar);

        final World targetWorld = targetLocation.getWorld();
        final double targetX = targetLocation.getX();
        final double targetY = targetLocation.getY();
        final double targetZ = targetLocation.getZ();
        
        final double initialDistance = player.getWorld().equals(targetWorld)
            ? player.getLocation().distance(targetLocation)
            : Math.sqrt(Math.pow(targetX - player.getLocation().getX(), 2) + Math.pow(targetZ - player.getLocation().getZ(), 2));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopGps(player);
                    return;
                }

                Location playerLoc = player.getLocation();
                double distance;
                
                if (ignoreHeight || !playerLoc.getWorld().equals(targetWorld)) {
                    distance = Math.sqrt(Math.pow(targetX - playerLoc.getX(), 2) + Math.pow(targetZ - playerLoc.getZ(), 2));
                } else {
                    distance = playerLoc.distance(new Location(targetWorld, targetX, targetY, targetZ));
                }

                int displayDistance = roundedDistance ? roundDistance(distance) : (int) distance;

                String direction = getDirectionArrow(playerLoc, targetX, targetZ);
                String msg = messageFormat
                    .replace("%distance%", String.valueOf(displayDistance))
                    .replace("%direction%", direction);

                bossBar.name(Message.messageBuilderComponent(msg));
                
                float progress = 1.0f - (float) Math.min(1.0, distance / initialDistance);
                if (roundedScale) {
                    progress = Math.round(progress * 10) / 10.0f;
                }
                bossBar.progress(Math.max(0f, Math.min(1.0f, progress)));
            }
        }.runTaskTimer(BAirDrop.getInstance(), 0, interval);

        activeSessions.put(player.getUniqueId(), new GpsSession(bossBar, task));
    }

    public static void stopGps(Player player) {
        GpsSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            player.hideBossBar(session.bossBar);
            session.task.cancel();
        }
    }

    public static void stopAll() {
        for (Map.Entry<UUID, GpsSession> entry : new HashMap<>(activeSessions).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue().bossBar);
            }
            entry.getValue().task.cancel();
        }
        activeSessions.clear();
    }

    private int roundDistance(double distance) {
        if (distance >= 1000) {
            return ((int) distance / 100) * 100;
        } else if (distance >= 500) {
            return ((int) distance / 50) * 50;
        } else if (distance >= 100) {
            return ((int) distance / 10) * 10;
        } else {
            return ((int) distance / 5) * 5;
        }
    }

    private String getDirectionArrow(Location playerLoc, double targetX, double targetZ) {
        double dx = targetX - playerLoc.getX();
        double dz = targetZ - playerLoc.getZ();
        
        double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
        if (targetAngle < 0) targetAngle += 360;
        
        float playerYaw = playerLoc.getYaw();
        if (playerYaw < 0) playerYaw += 360;
        
        double relativeAngle = targetAngle - playerYaw;
        if (relativeAngle < 0) relativeAngle += 360;
        if (relativeAngle >= 360) relativeAngle -= 360;
        
        if (relativeAngle >= 337.5 || relativeAngle < 22.5) return "⬆";
        if (relativeAngle >= 22.5 && relativeAngle < 67.5) return "⬈";
        if (relativeAngle >= 67.5 && relativeAngle < 112.5) return "➡";
        if (relativeAngle >= 112.5 && relativeAngle < 157.5) return "⬊";
        if (relativeAngle >= 157.5 && relativeAngle < 202.5) return "⬇";
        if (relativeAngle >= 202.5 && relativeAngle < 247.5) return "⬋";
        if (relativeAngle >= 247.5 && relativeAngle < 292.5) return "⬅";
        if (relativeAngle >= 292.5 && relativeAngle < 337.5) return "⬉";
        return "⬆";
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private record GpsSession(BossBar bossBar, BukkitTask task) {}
}
