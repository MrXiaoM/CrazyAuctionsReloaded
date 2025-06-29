package top.mrxiaom.crazyauctions.reloaded;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import top.mrxiaom.crazyauctions.reloaded.api.IRunTask;
import top.mrxiaom.crazyauctions.reloaded.api.IScheduler;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.currency.Vault;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.engine.MySQLEngine;
import top.mrxiaom.crazyauctions.reloaded.database.engine.SQLiteEngine;
import top.mrxiaom.crazyauctions.reloaded.database.storage.MySQLStorage;
import top.mrxiaom.crazyauctions.reloaded.database.storage.SQLiteStorage;
import top.mrxiaom.crazyauctions.reloaded.event.*;
import top.mrxiaom.crazyauctions.reloaded.util.*;

public class Main
    extends JavaPlugin 
{
    private static Main instance;
    public static Properties language = new Properties();
    
    private static final String lang = Locale.getDefault().toString();
    private GuiManager guiManager;
    private final IScheduler scheduler;
    public Main() {
        scheduler = new FoliaLibScheduler(this);
        instance = this;
    }
    
    public static Main getInstance() {
        return instance;
    }

    public IScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        if (MinecraftVersion.getVersion() == MinecraftVersion.UNKNOWN) {
            throw new IllegalStateException("当前服务端版本 (" + Bukkit.getServer().getBukkitVersion() + ") 不受支持");
        }
    }

    @Override
    public void onEnable() {
        AdventureUtil.init(this);
        LangUtilsHook.initialize();
        PAPI.initialize();
        guiManager = new GuiManager(this);
        long time = System.currentTimeMillis();
        if (lang.equalsIgnoreCase("zh_cn")) {
            try {
                language.load(getClass().getResourceAsStream("/Languages/Chinese.properties"));
            } catch (IOException ignored) {}
        } else {
            try {
                language.load(getClass().getResourceAsStream("/Languages/English.properties"));
            } catch (IOException ignored) {}
        }
        if (language.get("LanguageLoaded") != null) getServer().getConsoleSender().sendMessage(language.getProperty("LanguageLoaded").replace("&", "§"));
        
        if (!getDescription().getName().equals("CrazyAuctionsReloaded")) {
            if (language.get("PluginNameChange") != null) getServer().getConsoleSender().sendMessage(language.getProperty("PluginNameChange").replace("&", "§"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        PluginControl.reload(PluginControl.ReloadType.ALL);
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            if (language.get("DependentPluginExist") != null) getServer().getConsoleSender().sendMessage(language.getProperty("DependentPluginExist").replace("{plugin}", "Vault").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
        } else {
            if (language.get("DependentPluginNotExist") != null) getServer().getConsoleSender().sendMessage(language.getProperty("DependentPluginNotExist").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            return;
        }
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new Join(this), this);
        pm.registerEvents(new Quit(), this);
        pm.registerEvents(new EasyCommand(), this);
        pm.registerEvents(new ShopSign(), this);
        pm.registerEvents(new AuctionEvents(), this);
        registerCommandExecutor();
        reloadTimer();
        if (language.get("PluginEnabledSuccessfully") != null) getServer().getConsoleSender().sendMessage(language.getProperty("PluginEnabledSuccessfully").replace("{time}", String.valueOf(System.currentTimeMillis() - time)).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
        getScheduler().runTask(Vault::setupEconomy);
    }
    
    @Override
    public void onDisable() {
        if (guiManager != null) guiManager.onDisable();
        if (dataUpdateTask != null) dataUpdateTask.cancel();
        if (repricingTimeoutCheckTask != null) repricingTimeoutCheckTask.cancel();
        AuctionProcess.stop();
        GlobalMarket.getMarket().saveData();
        if (PluginControl.useMySQLStorage()) {
            try {
                if (MySQLEngine.getInstance().getConnection() != null && !MySQLEngine.getInstance().getConnection().isClosed()) {
                    MySQLStorage.cache.values().forEach(MySQLStorage::saveData);
                    if (language.get("MySQL-DataSave") != null) getServer().getConsoleSender().sendMessage(language.getProperty("MySQL-DataSave").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                }
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (PluginControl.useSQLiteStorage()) {
            try {
                if (SQLiteEngine.getInstance().getConnection() != null && !SQLiteEngine.getInstance().getConnection().isClosed()) {
                    SQLiteStorage.cache.values().forEach(SQLiteStorage::saveData);
                    if (language.get("SQLite-DataSave") != null) getServer().getConsoleSender().sendMessage(language.getProperty("SQLite-DataSave").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                }
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (PluginControl.automaticBackup()) {
            try {
                if (language.get("AutomaticBackupStarting") != null) getServer().getConsoleSender().sendMessage(language.getProperty("AutomaticBackupStarting").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                PluginControl.RollBackMethod.backup();
                if (language.get("AutomaticBackupDone") != null) getServer().getConsoleSender().sendMessage(language.getProperty("AutomaticBackupDone").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            } catch (Exception ex) {
                if (language.get("AutomaticBackupFailed") != null) getServer().getConsoleSender().sendMessage(language.getProperty("AutomaticBackupFailed").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            }
        }
        getScheduler().cancelTasks();
    }

    private IRunTask repricingTimeoutCheckTask;
    private IRunTask dataUpdateTask;
    
    public void reloadTimer() {
        if (dataUpdateTask != null) dataUpdateTask.cancel();
        if (repricingTimeoutCheckTask != null) repricingTimeoutCheckTask.cancel();
        if (PluginControl.isGlobalMarketAutomaticUpdate()) {
            final AtomicBoolean fault = new AtomicBoolean(false);
            long dataUpdateInterval = (long) (PluginControl.getGlobalMarketAutomaticUpdateDelay() * 1000) / 50L;
            dataUpdateTask = getScheduler().runTaskTimerAsync(() -> {
                try {
                    PluginControl.updateCacheData();
                    if (fault.get()) {
                        if (language.get("CacheUpdateReturnsToNormal") != null)
                            getServer().getConsoleSender().sendMessage(language.getProperty("CacheUpdateReturnsToNormal").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                        fault.set(false);
                    }
                } catch (Exception ex) {
                    if (language.get("CacheUpdateError") != null) {
                        getServer().getConsoleSender().sendMessage(language.getProperty("CacheUpdateError")
                                .replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null")
                                .replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                        getLogger().log(Level.WARNING, null, ex);
                    }
                    fault.set(true);
                    PluginControl.printStackTrace(ex);
                }
            }, dataUpdateInterval, dataUpdateInterval);
        } else {
            dataUpdateTask = null;
        }
        repricingTimeoutCheckTask = getScheduler().runTaskTimerAsync(() -> {
            long now = System.currentTimeMillis();
            for (UUID uuid : Lists.newArrayList(GuiManager.repricing.keySet())) {
                Object[] objects = GuiManager.repricing.get(uuid);
                long outdateTime = Long.parseLong(objects[1].toString());
                if (now >= outdateTime) continue;
                try {
                    MarketGoods mg  = (MarketGoods) objects[0];
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(p, "Repricing-Undo", placeholders);
                    }
                    GuiManager.repricing.remove(uuid);
                } catch (ClassCastException ignored) {}
            }
        }, 20L, 20L);
    }
    
    private void registerCommandExecutor() {
        PluginCommand command = getCommand("ca");
        if (command != null) {
            CrazyAuctionsCommand impl = new CrazyAuctionsCommand();
            command.setExecutor(impl);
            command.setTabCompleter(impl);
        }
        for (CrazyAuctionsSubCommandType subCommandType : CrazyAuctionsSubCommandType.values()) {
            CrazyAuctionsCommand.getSubCommands().put(subCommandType.getSubCommandName(), subCommandType.getSubCommand());
        }
    }
}