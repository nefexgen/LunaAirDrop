package org.by1337.bairdrop.hologram;

public enum HologramType {
    TEXTDISPLAY,
    ARMORSTAND;

    public static HologramType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
