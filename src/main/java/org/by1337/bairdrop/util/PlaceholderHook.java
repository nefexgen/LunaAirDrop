package org.by1337.bairdrop.util;

import org.bukkit.OfflinePlayer;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    @Override
    public @NotNull String getAuthor() {
        return "By1337";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "BAirDrop";
    }

    @Override
    public @NotNull String getVersion() {
        return BAirDrop.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {// %bairdrop_test% = test %bairdrop_time_to_open_<air id>%
        if (params.contains("time_to_open_")) { //%bairdrop_time_to_open_<air id>%
            String[] args = params.split("_");
            if (args.length != 4) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[3], null);
            if (airDrop == null) return "error";
            return airDrop.getTimeToOpen() + "";
        }
        if (params.equals("time_start")) { //%bairdrop_time_start%
            if (BAirDrop.globalTimer == null)
                return AirManager.getTimeToNextAirdrop() + "";
            int time = 0;
            time += BAirDrop.globalTimer.getTimeToStart();
            if (BAirDrop.globalTimer.getAir() != null)
                time += BAirDrop.globalTimer.getAir().getTimeToStart();
            return time + "";
        }
        if (params.equals("time_start_format")) { //%bairdrop_time_start_format%
            if (BAirDrop.globalTimer == null)
                return AirManager.getFormat(AirManager.getTimeToNextAirdrop());
            int time = 0;
            time += BAirDrop.globalTimer.getTimeToStart();
            if (BAirDrop.globalTimer.getAir() != null)
                time += BAirDrop.globalTimer.getAir().getTimeToStart();
            return AirManager.getFormat(time);
        }
        if (params.equals("near")) { //%bairdrop_near%
            if (player == null) return "";
            AirDrop airDrop = null;
            int dist = 0;
            for (AirDrop air : BAirDrop.airDrops.values()) {
                if (!air.isAirDropStarted()) continue;
                if (!air.getAnyLoc().getWorld().equals(player.getPlayer().getWorld())) continue;
                if (dist > player.getPlayer().getLocation().distance(air.getAirDropLocation()) || airDrop == null) {
                    dist = (int) player.getPlayer().getLocation().distance(air.getAirDropLocation());
                    airDrop = air;
                }
            }
            if (airDrop == null)
                return BAirDrop.getConfigMessage().getMessage("air-near-none");
            return Message.messageBuilder(airDrop.replaceInternalPlaceholder(BAirDrop.getConfigMessage().getMessage("air-near").replace("{dist}", dist + "")));
        }
        if (params.contains("time_to_end_format_")) { //%bairdrop_time_to_end_format_<air id>%
            String[] args = params.split("_");
            if (args.length != 5) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[4], null);
            if (airDrop == null) return "error";
            return AirManager.getFormat(airDrop.getTimeStop());
        }
        if (params.contains("time_to_start_format_")) { //%bairdrop_time_to_start_format_<air_id>%
            String[] args = params.split("_");
            if (args.length != 5) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[4], null);
            if (airDrop == null) return "error";
            return  AirManager.getFormat(airDrop.getTimeToStart());
        }
        if (params.contains("time_to_start_")) { //%bairdrop_time_to_start_<air_id>%
            String[] args = params.split("_");
            if (args.length != 4) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[3], null);
            if (airDrop == null) return "error";
            return airDrop.getTimeToStart() + "";
        }
        if (params.contains("time_to_end_")) { //%bairdrop_time_to_end_<air id>%
            String[] args = params.split("_");
            if (args.length != 4) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[3], null);
            if (airDrop == null) return "error";
            return airDrop.getTimeStop() + "";
        }
        if (params.contains("air_name_")) { //%bairdrop_air_name_<air id>%
            String[] args = params.split("_");
            if (args.length != 3) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[2], null);
            if (airDrop == null) return "error";
            return airDrop.getDisplayName();
        }
        if (params.contains("x_")) { //%bairdrop_x_<air id>%
            String[] args = params.split("_");
            if (args.length != 2) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[1], null);
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted() || airDrop.getAnyLoc() == null) {
                return "?";
            }
            return String.valueOf((int) airDrop.getAnyLoc().getX());
        }
        if (params.contains("y_")) { //%bairdrop_y_<air id>%
            String[] args = params.split("_");
            if (args.length != 2) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[1], null);
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted() || airDrop.getAnyLoc() == null) {
                return "?";
            }
            return String.valueOf((int) airDrop.getAnyLoc().getY());
        }
        if (params.contains("z_")) { //%bairdrop_z_<air id>%
            String[] args = params.split("_");
            if (args.length != 2) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[1], null);
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted() || airDrop.getAnyLoc() == null) {
                return "?";
            }
            return String.valueOf((int) airDrop.getAnyLoc().getZ());
        }
        if (params.contains("world_")) { //%bairdrop_world_<air id>%
            String[] args = params.split("_");
            if (args.length != 2) return "error";
            AirDrop airDrop = BAirDrop.airDrops.getOrDefault(args[1], null);
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted() || airDrop.getAnyLoc() == null || airDrop.getAnyLoc().getWorld() == null) {
                return "?";
            }
            return airDrop.getAnyLoc().getWorld().getName();
        }
        
        String result = handleTimeUnitPlaceholder(params);
        if (result != null) return result;
        
        return null;
    }
    
    private String handleTimeUnitPlaceholder(String params) {
        String[] timeTypes = {"days", "hours", "minutes", "seconds", "total_seconds"};
        String[] eventTypes = {"to_start", "to_end", "to_open"};
        
        for (String timeType : timeTypes) {
            for (String eventType : eventTypes) {
                String prefix = timeType + "_" + eventType + "_";
                if (params.startsWith(prefix)) {
                    return processTimeUnit(params, prefix, timeType, eventType);
                }
            }
        }
        return null;
    }
    
    private String processTimeUnit(String params, String prefix, String timeType, String eventType) {
        String remainder = params.substring(prefix.length());
        
        String airDropId;
        String fallback = null;
        
        int underscoreIndex = remainder.indexOf('_');
        if (underscoreIndex != -1) {
            airDropId = remainder.substring(0, underscoreIndex);
            fallback = remainder.substring(underscoreIndex + 1);
        } else {
            airDropId = remainder;
        }
        
        AirDrop airDrop = BAirDrop.airDrops.getOrDefault(airDropId, null);
        if (airDrop == null) return "error";
        
        int totalSeconds = switch (eventType) {
            case "to_start" -> airDrop.getTimeToStart();
            case "to_end" -> airDrop.getTimeStop();
            case "to_open" -> airDrop.getTimeToOpen();
            default -> 0;
        };
        
        int value = switch (timeType) {
            case "days" -> totalSeconds / 86400;
            case "hours" -> (totalSeconds % 86400) / 3600;
            case "minutes" -> (totalSeconds % 3600) / 60;
            case "seconds" -> totalSeconds % 60;
            case "total_seconds" -> totalSeconds;
            default -> 0;
        };
        
        if (fallback != null && value == 0) {
            return fallback.isEmpty() ? "" : Message.messageBuilder(fallback);
        }
        
        return String.valueOf(value);
    }


}
