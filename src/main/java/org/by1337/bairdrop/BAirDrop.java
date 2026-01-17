package org.by1337.bairdrop;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.by1337.bairdrop.configManager.CConfig;
import org.by1337.bairdrop.configManager.ConfigMessage;
import org.by1337.bairdrop.configManager.Config;
import org.by1337.bairdrop.hologram.HologramManager;
import org.by1337.bairdrop.listeners.Compass;
import org.by1337.bairdrop.listeners.CraftItem;
import org.by1337.bairdrop.listeners.InteractListener;
import org.by1337.bairdrop.summoner.Summoner;
import org.by1337.bairdrop.worldGuardHook.RegionManager;
import org.by1337.bairdrop.command.Commands;
import org.by1337.bairdrop.command.Completer;
import org.by1337.bairdrop.command.DelayCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.by1337.bairdrop.customListeners.CustomEvent;
import org.by1337.bairdrop.customListeners.observer.Observer;
import org.by1337.bairdrop.effect.effectImpl.*;
import org.by1337.bairdrop.serializable.EffectDeserialize;
import org.by1337.bairdrop.serializable.StateSerializable;
import org.by1337.bairdrop.util.*;
import org.by1337.bairdrop.util.Message;

import java.io.*;

import java.util.*;


public final class BAirDrop extends JavaPlugin {

    public static HashMap<String, AirDrop> airDrops = new HashMap<>();
    public static HashMap<NamespacedKey, Observer> customEventListeners = new HashMap<>();

    public static Summoner summoner = new Summoner();
    public static GlobalTimer globalTimer;
    public static HashMap<String, CustomCraft> crafts = new HashMap<>();
    public static Compass compass;
    public static LogLevel logLevel;
    private static Config config;
    private static ConfigMessage configMessage;
    private static BAirDrop instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        EffectDeserialize.register(Circle.class);
        EffectDeserialize.register(ExpandingCircle.class);
        EffectDeserialize.register(FireworkEffect.class);
        EffectDeserialize.register(Helix.class);
        EffectDeserialize.register(RandomParticle.class);
        EffectDeserialize.register(Torus.class);
        EffectDeserialize.register(WrithingHelix.class);

        config = new CConfig();

        configMessage = (ConfigMessage) config;
        getInstance().saveDefaultConfig();
        getInstance().saveConfig();


        try {
            String lvl = getInstance().getConfig().getString("log-level", "LOW");
            logLevel = LogLevel.valueOf(lvl);
        } catch (IllegalArgumentException e) {
            Message.error(e.getLocalizedMessage());
            logLevel = LogLevel.LOW;
        }

        long x = System.currentTimeMillis();

        getiConfig().loadConfiguration();

        if (this.getConfig().getBoolean("use-metrics"))
            new Metrics(getInstance(), 17870);

        Objects.requireNonNull(getInstance().getCommand("bairdrop")).setExecutor(new Commands());
        Objects.requireNonNull(getInstance().getCommand("bairdrop")).setTabCompleter(new Completer());
        registerDelayAliases();
        Bukkit.getServer().getPluginManager().registerEvents(new InteractListener(), getInstance());
        getServer().getPluginManager().registerEvents(summoner, getInstance());
        getServer().getPluginManager().registerEvents(new CraftItem(), BAirDrop.getInstance());
        getServer().getPluginManager().registerEvents(compass, BAirDrop.getInstance());


        for (File file : getiConfig().getAirDrops().keySet()) {
            AirDrop airDrop = new CAirDrop(getiConfig().getAirDrops().get(file), file);
            airDrops.put(getiConfig().getAirDrops().get(file).getString("air-id"), airDrop);
            if(instance.getConfig().getBoolean("state-serializable")){
                StateSerializable stateSerializable = (StateSerializable) airDrop;
                stateSerializable.stateDeserialize();
            }

        }

        List<String> ids = new ArrayList<>(airDrops.keySet());
        for (String id : ids) {
            airDrops.get(id).registerAllSignedObservers();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new PlaceholderHook().register();

        if (BAirDrop.getInstance().getConfig().getBoolean("global-time.enable")) {
            globalTimer = new GlobalTimer((BAirDrop.getInstance().getConfig().getInt("global-time.time") * 60));
        }
        if (logLevel == LogLevel.HARD) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Message.debug(String.format(getConfigMessage().getMessage("thread-count"), getInstance().getServer().getScheduler().getPendingTasks().stream().filter(t -> t.getOwner().getName().equalsIgnoreCase("BairDropS"))
                            .count()), LogLevel.HARD);
                }
            }.runTaskTimerAsynchronously(getInstance(), 10, 10);
        }

        ExecuteCommands.registerIgnoreCommand("[SCHEDULER]");
        ExecuteCommands.registerIgnoreCommand("[ASYNC]");
        ExecuteCommands.registerIgnoreCommand("[LATER-");

        Message.logger(String.format(getConfigMessage().getMessage("start-time"), System.currentTimeMillis() - x));

    }


    public static Config getiConfig() {
        return config;
    }

    public static ConfigMessage getConfigMessage() {
        return configMessage;
    }

    public static BAirDrop getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        if (!getiConfig().isLoaded())
            return;
        long x = System.currentTimeMillis();
        for (AirDrop airDrop : airDrops.values()) {
            if (instance.getConfig().getBoolean("state-serializable") && airDrop instanceof StateSerializable stateSerializable) stateSerializable.stateSerialize();

            if (airDrop.isAirDropStarted())
                airDrop.End();
            if (airDrop.isClone())
                airDrop.End();
            airDrop.notifyObservers(CustomEvent.UNLOAD, null);
            HologramManager.remove(airDrop.getId());
            airDrop.save();
            airDrop.schematicsUndo();
            RegionManager.RemoveRegion(airDrop);
        }
        CustomCraft.unloadCrafts();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new PlaceholderHook().unregister();
        Message.logger(String.format(getConfigMessage().getMessage("off-time"), System.currentTimeMillis() - x));

    }

    /**
     * Перезагрузка конфиг файлов
     */
    public static void reload() {
        for (AirDrop airDrop : airDrops.values()) {
            if (airDrop.isAirDropStarted())
                airDrop.End();
            if (airDrop.isClone())
                airDrop.End();
            airDrop.notifyObservers(CustomEvent.UNLOAD, null);
            HologramManager.remove(airDrop.getId());
            airDrop.setCanceled(true);
            airDrop.schematicsUndo();
            RegionManager.RemoveRegion(airDrop);
        }
        CustomCraft.unloadCrafts();
        getiConfig().getSchematics().clear();
        airDrops.clear();
        customEventListeners.clear();
        // EffectFactory.EffectList.clear();
        getiConfig().getAirDrops().clear();

        getInstance().reloadConfig();
        compass.loadItem();
        summoner.LoadSummoner();
        if (globalTimer != null) {
            if (!BAirDrop.getInstance().getConfig().getBoolean("global-time.enable")) {
                globalTimer.setStop(true);
                globalTimer = null;
            } else {
                globalTimer.setTimeToStartCons(BAirDrop.getInstance().getConfig().getInt("global-time.time") * 60);
                globalTimer.setTimeToStart(globalTimer.getTimeToStartCons());
                if (globalTimer.getAir() != null)
                    globalTimer.setAir(null);
            }
        } else if (BAirDrop.getInstance().getConfig().getBoolean("global-time.enable")) {
            globalTimer = new GlobalTimer((BAirDrop.getInstance().getConfig().getInt("global-time.time") * 60));
        }
        getiConfig().loadConfiguration();
        for (File file : getiConfig().getAirDrops().keySet()) {
            airDrops.put(getiConfig().getAirDrops().get(file).getString("air-id"), new CAirDrop(getiConfig().getAirDrops().get(file), file));
        }
        List<String> ids = new ArrayList<>(airDrops.keySet());
        for (String id : ids) {
            airDrops.get(id).registerAllSignedObservers();
        }
    }

    private void registerDelayAliases() {
        List<String> aliases = getConfig().getStringList("delay-command.aliases");
        if (aliases.isEmpty()) return;
        
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            
            DelayCommand delayCommand = new DelayCommand();
            for (String alias : aliases) {
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                PluginCommand cmd = constructor.newInstance(alias, this);
                cmd.setExecutor(delayCommand);
                cmd.setTabCompleter(delayCommand);
                cmd.setPermission("bair.delay");
                commandMap.register(getName(), cmd);
            }
        } catch (Exception e) {
            Message.error("Failed to register delay command aliases: " + e.getMessage());
        }
    }

}

