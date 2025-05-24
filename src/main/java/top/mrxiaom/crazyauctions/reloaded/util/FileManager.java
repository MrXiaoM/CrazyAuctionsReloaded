package top.mrxiaom.crazyauctions.reloaded.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;
import top.mrxiaom.crazyauctions.reloaded.database.StorageMethod;
import top.mrxiaom.crazyauctions.reloaded.database.engine.MySQLEngine;
import top.mrxiaom.crazyauctions.reloaded.database.engine.SQLiteEngine;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;
import top.mrxiaom.crazyauctions.reloaded.util.enums.Version;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {
    
    private static final FileManager instance = new FileManager();
    private static boolean doingBackup = false;
    private static boolean doingRollBack = false;
    private static boolean syncing = false;
    private Main main;
    private String prefix = "[CrazyAuctionsReloaded] ";
    private Boolean log = false;
    private final HashMap<Files, File> files = new HashMap<>();
    private final HashMap<Files, FileConfiguration> configurations = new HashMap<>();

    public static FileManager getInstance() {
        return instance;
    }

    private static CommandSender[] syncSenders = {};
    
    public static Runnable synchronizeThread = () -> {
        syncing = true;
        
        // Old Data Files
        File database_File = new File("plugins/CrazyAuctionsReloaded/Database.yml");
        File data_File = new File("plugins/CrazyAuctionsReloaded/Data.yml");
        File data_File_On_CrazyAuctions = new File("plugins/CrazyAuctionsReloaded/Data.yml");
        File[] files = {database_File, data_File, data_File_On_CrazyAuctions};
        
        GlobalMarket market = GlobalMarket.getMarket();
        
        try {
            for (File file : files) {
                if (file != null && file.exists()) {
                    YamlConfiguration databaseFile = new YamlConfiguration();
                    ConfigurationSection section;
                    try (InputStream input = java.nio.file.Files.newInputStream(file.toPath());
                         Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        databaseFile.load(reader);
                    }
                    section = databaseFile.getConfigurationSection("Items");
                    if (section != null) for (String key : section.getKeys(false)) {
                        ShopType type = ShopType.SELL;
                        if (databaseFile.getBoolean("Items." + key + ".Biddable")) {
                            type = ShopType.BID;
                        } else if (databaseFile.getBoolean("Items." + key + ".Buyable")) {
                            type = ShopType.BUY;
                        }
                        ItemOwner owner;
                        String itemOwner = databaseFile.getString("Items." + key + ".Owner");
                        String itemSeller = databaseFile.getString("Items." + key + ".Seller");
                        if (itemOwner != null) {
                            String[] info = itemOwner.split(":");
                            owner = new ItemOwner(UUID.fromString(info[1]), info[0]);
                        } else if (itemSeller != null) {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(itemSeller);
                            if (op == null) continue;
                            owner = new ItemOwner(op.getUniqueId(), op.getName());
                        } else {
                            continue;
                        }
                        ItemStack is;
                        if (databaseFile.get("Items." + key + ".Item") != null) {
                            is = databaseFile.getItemStack("Items." + key + ".Item");
                        } else {
                            continue;
                        }
                        double money = databaseFile.getDouble("Items." + key + ".Price");
                        if (type.equals(ShopType.BUY)) {
                            money = databaseFile.getDouble("Items." + key + ".Reward");
                        }
                        MarketGoods mg;
                        if (type.equals(ShopType.BID)) {
                            String topBidder = databaseFile.get("Items." + key + ".TopBidder") != null ? databaseFile.getString("Items." + key + ".TopBidder") : "None";
                            mg = new MarketGoods(
                                    market.makeUID(),
                                    type,
                                    owner,
                                    is,
                                    databaseFile.getLong("Items." + key + ".Time-Till-Expire"),
                                    databaseFile.getLong("Items." + key + ".Full-Time"),
                                    databaseFile.get("Items." + key + ".Added-Time") != null ? databaseFile.getLong("Items." + key + ".Added-Time") : -1,
                                    money,
                                    topBidder
                            );
                        } else {
                            mg = new MarketGoods(
                                    market.makeUID(),
                                    type,
                                    owner,
                                    is,
                                    databaseFile.getLong("Items." + key + ".Time-Till-Expire"),
                                    databaseFile.getLong("Items." + key + ".Full-Time"),
                                    databaseFile.get("Items." + key + ".Added-Time") != null ? databaseFile.getLong("Items." + key + ".Added-Time") : -1,
                                    money
                            );
                        }
                        market.addGoods(mg);
                    }

                    section = databaseFile.getConfigurationSection("OutOfTime/Cancelled");
                    if (section != null) for (String key : section.getKeys(false)) {
                        if (databaseFile.get("OutOfTime/Cancelled." + key + ".Item") != null) {
                            String itemOwner = databaseFile.getString("OutOfTime/Cancelled." + key + ".Owner");
                            String itemSeller = databaseFile.getString("OutOfTime/Cancelled." + key + ".Seller");
                            if (itemOwner != null) {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(itemOwner.split(":")[1]));
                                if (op != null) {
                                    Storage playerData = Storage.getPlayer(op);
                                    ItemMail im = new ItemMail(playerData.makeUID(), Bukkit.getOfflinePlayer(playerData.getUUID()), databaseFile.getItemStack("OutOfTime/Cancelled." + key + ".Item"), databaseFile.getLong("OutOfTime/Cancelled." + key + ".Full-Time"), -1, databaseFile.getBoolean("OutOfTime/Cancelled." + key + ".Never-Expire"));
                                    playerData.addItem(im);
                                }
                            } else if (itemSeller != null) {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(itemSeller);
                                if (op != null) {
                                    Storage playerData = Storage.getPlayer(op);
                                    ItemMail im = new ItemMail(playerData.makeUID(), Bukkit.getOfflinePlayer(playerData.getUUID()), databaseFile.getItemStack("OutOfTime/Cancelled." + key + ".Item"), databaseFile.getLong("OutOfTime/Cancelled." + key + ".Full-Time"), -1, databaseFile.getBoolean("OutOfTime/Cancelled." + key + ".Never-Expire"));
                                    playerData.addItem(im);
                                }
                            }
                        }
                    }
                }
            }
            for (CommandSender sender : FileManager.syncSenders) {
                if (sender != null) {
                    MessageUtil.sendMessage(sender, "Admin-Command.Synchronize.Successfully");
                }
            }
            syncing = false;
        } catch (Exception ex) {
            for (CommandSender sender : FileManager.syncSenders) {
                if (sender != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%error%", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null");
                    MessageUtil.sendMessage(sender, "Admin-Command.Synchronize.Failed", placeholders);
                }
            }
            syncing = false;
            PluginControl.printStackTrace(ex);
        }
    };
    
    private static CommandSender[] backupSenders = {};
    
    public static Runnable backupThread = () -> {
        try {
            doingBackup = true;
            String fileName = MessageUtil.getValue("Admin-Command.Backup.Backup-Name").replace("%date%", new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())) + ".db";
            GlobalMarket market = GlobalMarket.getMarket();
            File folder = new File("plugins/CrazyAuctionsReloaded/Backup");
            if (!folder.exists()) folder.mkdir();
            File file = new File(folder, fileName);
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            try (Connection DBFile = DriverManager.getConnection("jdbc:sqlite:plugins/CrazyAuctionsReloaded/Backup/" + fileName)) {
                DBFile.prepareStatement("CREATE TABLE IF NOT EXISTS ItemMail" +
                    "("
                    + "UUID VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "Name VARCHAR(16) NOT NULL,"
                    + "YamlData LONGTEXT" +
                    ");").executeUpdate();
                DBFile.prepareStatement("CREATE TABLE IF NOT EXISTS Market" +
                    "("
                    + "YamlMarket LONGTEXT" +
                    ");").executeUpdate();
                PreparedStatement statement = DBFile.prepareStatement("INSERT INTO Market (YamlMarket) VALUES(?)");
                statement.setString(1, market.getYamlData().saveToString());
                statement.executeUpdate();
                if (PluginControl.useSplitDatabase()) {
                    switch (PluginControl.getItemMailStorageMethod()) {
                        case MySQL: {
                            MySQLEngine.backupPlayerData(DBFile);
                            break;
                        }
                        case SQLite: {
                            SQLiteEngine.backupPlayerData(DBFile);
                            break;
                        }
                        case YAML:
                        default: {
                            File playerFolder = new File("plugins/CrazyAuctionsReloaded/Players/");
                            if (playerFolder.exists()) {
                                File[] files = playerFolder.listFiles();
                                if (files != null) for (File f : files) {
                                    if (f.getName().endsWith(".yml")) {
                                        YamlConfiguration yaml = new YamlConfiguration();
                                        try {
                                            yaml.load(f);
                                        } catch (IOException | InvalidConfigurationException ex) {
                                            PluginControl.printStackTrace(ex);
                                            continue;
                                        }
                                        PreparedStatement ps = DBFile.prepareStatement("INSERT INTO ItemMail (Name, UUID, YamlData) VALUES(?, ?, ?)");
                                        ps.setString(1, yaml.get("Name") != null ? yaml.getString("Name") : "null");
                                        ps.setString(2, f.getName());
                                        ps.setString(3, yaml.get("Items") != null ? yaml.saveToString() : "{}");
                                        ps.executeUpdate();
                                    }
                                }
                            }
                            break;
                        }
                    }
                } else if (PluginControl.useMySQLStorage()) {
                    MySQLEngine.backupPlayerData(DBFile);
                } else if (PluginControl.useSQLiteStorage()) {
                    SQLiteEngine.backupPlayerData(DBFile);
                } else {
                    File playerFolder = new File("plugins/CrazyAuctionsReloaded/Players/");
                    if (playerFolder.exists()) {
                        File[] files = playerFolder.listFiles();
                        if (files != null) for (File f : files) {
                            if (f.getName().endsWith(".yml")) {
                                YamlConfiguration yaml = new YamlConfiguration();
                                try {
                                    yaml.load(f);
                                } catch (IOException | InvalidConfigurationException ex) {
                                    PluginControl.printStackTrace(ex);
                                    continue;
                                }
                                PreparedStatement ps = DBFile.prepareStatement("INSERT INTO ItemMail (Name, UUID, YamlData) VALUES(?, ?, ?)");
                                ps.setString(1, yaml.get("Name") != null ? yaml.getString("Name") : "null");
                                ps.setString(2, f.getName());
                                ps.setString(3, yaml.get("Items") != null ? yaml.saveToString() : "{}");
                                ps.executeUpdate();
                            }
                        }
                    }
                }
            }
            for (CommandSender sender : FileManager.backupSenders) {
                if (sender != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%file%",  fileName);
                    MessageUtil.sendMessage(sender, "Admin-Command.Backup.Successfully", placeholders);
                }
            }
            doingBackup = false;
        } catch (Exception ex) {
            for (CommandSender sender : FileManager.backupSenders) {
                if (sender != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%error%",  ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null");
                    MessageUtil.sendMessage(sender, "Admin-Command.Backup.Failed", placeholders);
                }
            }
            doingBackup = false;
            PluginControl.printStackTrace(ex);
        }
    };
    
    /**
     * Synchronize command.
     */
    public static void synchronize(CommandSender... sender) {
        syncSenders = sender;
        new Thread(synchronizeThread, "SynchronizeThread").start();
    }
    
    /**
     * Backup command.
     */
    public static void backup(CommandSender... sender) {
        backupSenders = sender;
        new Thread(backupThread, "BackupThread").start();
    }
    
    public static boolean isBackingUp() {
        return doingBackup;
    }
    
    public static boolean isSyncing() {
        return syncing;
    }
    
    public static boolean isRollingBack() {
        return doingRollBack;
    }
    
    /**
     * Roll Back command.
     * The rollback will cover all the current data,
     * and call a large number of IO read and write performance.
     * To ensure that the data is error-free,
     * it cannot be rolled back asynchronously.
     */
    public static void rollBack(File backupFile, CommandSender... sender) {
        doingRollBack = true;
        new PluginControl.RollBackMethod(backupFile, instance, sender).rollBack(true);
        doingRollBack = false;
    }
    
    public void saveResource(Files file) {
        String lang = Locale.getDefault().toString();
        String path = "English";
        if (lang.equalsIgnoreCase("zh_cn")) {
            path = "Chinese";
        }
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        if (!newFile.exists()) {
            try {
                String fileLocation = file.getFileLocation();
                //Switch between 1.12.2- and 1.13+ config version.
                if (file == Files.CONFIG) {
                    if (Version.getCurrentVersion().isOlder(Version.v1_13_R2)) {
                        fileLocation = "Config1.12.2-Down.yml";
                    } else {
                        fileLocation = "Config1.13-Up.yml";
                    }
                }
                File serverFile = new File(main.getDataFolder(), file.getFileLocation());
                InputStream jarFile = getClass().getResourceAsStream("/Languages/" + path + "/" + fileLocation);
                saveFile(jarFile, serverFile); 
            } catch (IOException ex) {
                if (Main.language.get("ConfigurationFileNotExist") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileNotExist").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
                PluginControl.printStackTrace(ex);
                return;
            }
        }
        files.put(file, newFile);
    }
    
    private void saveFile(InputStream is, File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (OutputStream out = java.nio.file.Files.newOutputStream(file.toPath())) {
            int b;
            while ((b = is.read()) != -1) {
                out.write((char) b);
            }
        }
    }
    
    public void reloadMessages() {
        Files file = Files.MESSAGES;
        saveResource(file);
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration config = new YamlConfiguration();
            config.load(Config);
            configurations.put(file, config);
            if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
        } catch (IOException | InvalidConfigurationException ex) {
            if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
            saveResource(file);
            PluginControl.printStackTrace(ex);
            try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                FileConfiguration config = new YamlConfiguration();
                config.load(newConfig);
                configurations.put(file, config);
            } catch (IOException | InvalidConfigurationException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
        }
    }
    
    public void reloadConfig() {
        Files file = Files.CONFIG;
        File oldconfig = new File(main.getDataFolder(), "config.yml");
        if (oldconfig.exists()) {
            oldconfig.renameTo(new File(main.getDataFolder(), "Config.yml"));
        }
        saveResource(file);
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration config = new YamlConfiguration();
            config.load(Config);
            configurations.put(file, config);
            if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
        } catch (IOException | InvalidConfigurationException ex) {
            if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
            saveResource(file);
            PluginControl.printStackTrace(ex);
            try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                FileConfiguration config = new YamlConfiguration();
                config.load(newConfig);
                configurations.put(file, config);
            } catch (IOException | InvalidConfigurationException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
        }
    }
    
    public void reloadDatabaseFile() {
        if ((PluginControl.useMySQLStorage() && PluginControl.useSplitDatabase() && PluginControl.getMarketStorageMethod().equals(StorageMethod.MySQL)) 
                || PluginControl.useSQLiteStorage() && PluginControl.useSplitDatabase() && PluginControl.getMarketStorageMethod().equals(StorageMethod.SQLite)) return;
        Files file = Files.DATABASE;
        saveResource(file);
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration config = new YamlConfiguration();
            config.load(Config);
            configurations.put(file, config);
            if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
        } catch (IOException | InvalidConfigurationException ex) {
            if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
            saveResource(file);
            PluginControl.printStackTrace(ex);
            try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                FileConfiguration config = new YamlConfiguration();
                config.load(newConfig);
                configurations.put(file, config);
            } catch (IOException | InvalidConfigurationException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
        }
        GlobalMarket.getMarket().reloadData();
    }
    
    public void reloadCategoryFile() {
        Files file = Files.CATEGORY;
        saveResource(file);
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration config = new YamlConfiguration();
            config.load(Config);
            configurations.put(file, config);
            if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
        } catch (IOException | InvalidConfigurationException ex) {
            if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
            saveResource(file);
            PluginControl.printStackTrace(ex);
            try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                FileConfiguration config = new YamlConfiguration();
                config.load(newConfig);
                configurations.put(file, config);
            } catch (IOException | InvalidConfigurationException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
        }
        GlobalMarket.getMarket().reloadData();
    }
    
    public void reloadItemCollectionFile() {
        Files file = Files.ITEM_COLLECTION;
        saveResource(file);
        File newFile = new File(main.getDataFolder(), file.getFileLocation());
        try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration config = new YamlConfiguration();
            config.load(Config);
            configurations.put(file, config);
            if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
        } catch (IOException | InvalidConfigurationException ex) {
            if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
            saveResource(file);
            PluginControl.printStackTrace(ex);
            try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                FileConfiguration config = new YamlConfiguration();
                config.load(newConfig);
                configurations.put(file, config);
            } catch (IOException | InvalidConfigurationException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
        }
        GlobalMarket.getMarket().reloadData();
    }
    
    public FileManager setup(Main main) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (GUI.openingGUI.containsKey(player.getUniqueId())) {
                        player.closeInventory();
                    }
                }
            }
        }.runTask(main);
        prefix = "[" + main.getName() + "] ";
        this.main = main;
        if (!main.getDataFolder().exists()) {
            main.getDataFolder().mkdirs();
        }
        files.clear();
        //Loads all the normal static files.
        for (Files file : Files.values()) {
            if (file.equals(Files.DATABASE)) {
                if (PluginControl.useMySQLStorage()) {
                    if (PluginControl.useSplitDatabase()) {
                        if (PluginControl.getMarketStorageMethod().equals(StorageMethod.MySQL)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                if (PluginControl.useSQLiteStorage()) {
                    if (PluginControl.useSplitDatabase()) {
                        if (PluginControl.getMarketStorageMethod().equals(StorageMethod.SQLite)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
            File oldconfig = new File(main.getDataFolder(), "config.yml");
            if (oldconfig.exists()) {
                oldconfig.renameTo(new File(main.getDataFolder(), "Config.yml"));
            }
            saveResource(file);
            File newFile = new File(main.getDataFolder(), file.getFileLocation());
            try (Reader Config = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()), StandardCharsets.UTF_8)) {
                FileConfiguration config = new YamlConfiguration();
                config.load(Config);
                configurations.put(file, config);
                if (Main.language.get("ConfigurationFileLoadedSuccessfully") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadedSuccessfully").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
            } catch (IOException | InvalidConfigurationException ex) {
                if (Main.language.get("ConfigurationFileLoadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileLoadingError").replace("{file}", newFile.getName()).replace("{prefix}", prefix).replace("&", "§"));
                File oldFile = new File(main.getDataFolder(), newFile.getName() + ".old");
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                newFile.renameTo(oldFile);
                saveResource(file);
                PluginControl.printStackTrace(ex);
                try (Reader newConfig = new InputStreamReader(java.nio.file.Files.newInputStream(newFile.toPath()))) {
                    FileConfiguration config = new YamlConfiguration();
                    config.load(newConfig);
                    configurations.put(file, config);
                } catch (IOException | InvalidConfigurationException ex1) {
                    PluginControl.printStackTrace(ex1);
                }
                if (Main.language.get("ConfigurationFileRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("ConfigurationFileRepair").replace("{prefix}", prefix).replace("&", "§"));
            }
        }
        return this;
    }
    
    /**
     * Turn on the logger system for the FileManager.
     * @param log True to turn it on and false for it to be off.
     */
    public FileManager logInfo(Boolean log) {
        this.log = log;
        return this;
    }
    
    /**
     * Check if the logger is logging in console.
     * @return True if it is and false if it isn't.
     */
    public Boolean isLogging() {
        return log;
    }

    /**
     * Gets the file from the system.
     * @return The file from the system.
     */
    public FileConfiguration getFile(Files file) {
        return configurations.get(file);
    }

    /**
     * Saves the file from the loaded state to the file system.
     */
    public void saveFile(Files file) {
        try {
            configurations.get(file).save(files.get(file));
        } catch (IOException e) {
            System.out.println(prefix + "Could not save " + file.getFileName() + "!");
            PluginControl.printStackTrace(e);
        }
    }

    /**
     * Overrides the loaded state file and loads the file systems file.
     */
    public void reloadFile(Files file) {
        configurations.put(file, YamlConfiguration.loadConfiguration(files.get(file)));
    }

    public enum Files {
        
        //ENUM_NAME("FileName.yml", "FilePath.yml"),
        CONFIG("Config.yml", "Config.yml"),
        DATABASE("Database.yml", "Database.yml"),
        CATEGORY("Category.yml", "Category.yml"),
        ITEM_COLLECTION("ItemCollection.yml", "ItemCollection.yml"),
        MESSAGES("Messages.yml", "Messages.yml");
        
        private final String fileName;
        private final String fileLocation;
        
        /**
         * The files that the server will try and load.
         * @param fileName The file name that will be in the plugin's folder.
         * @param fileLocation The location the file is in while in the Jar.
         */
        Files(String fileName, String fileLocation) {
            this.fileName = fileName;
            this.fileLocation = fileLocation;
        }
        
        /**
         * Get the name of the file.
         * @return The name of the file.
         */
        public String getFileName() {
            return fileName;
        }
        
        /**
         * The location the jar it is at.
         * @return The location in the jar the file is in.
         */
        public String getFileLocation() {
            return fileLocation;
        }

        public ProtectedConfiguration getFile() {
            return new ProtectedConfiguration(this);
        }
        
        /**
         * Saves the file from the loaded state to the file system.
         */
        public void saveFile() {
            getInstance().saveFile(this);
        }
        
        /**
         * Overrides the loaded state file and loads the file systems file.
         */
        public void reloadFile() {
            getInstance().reloadFile(this);
        }
    }
    
    public static class ProtectedConfiguration {
        private final FileConfiguration config;
        private final Files file;
        
        private static final Map<Files, FileConfiguration> defaultConfig = new HashMap<>();
        
        private ProtectedConfiguration(Files file) {
            this.file = file;
            config = getInstance().getFile(file);
        }

        public FileConfiguration getConfig() {
            return config;
        }

        public Object get(String path) {
            return config.get(path);
        }
        
        public String getString(String path) {
            if (file.equals(Files.DATABASE)) return config.getString(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getString(path);
        }
        
        public int getInt(String path) {
            if (file.equals(Files.DATABASE)) return config.getInt(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getInt(path);
        }
        
        public double getDouble(String path) {
            if (file.equals(Files.DATABASE)) return config.getDouble(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getDouble(path);
        }
        
        public long getLong(String path) {
            if (file.equals(Files.DATABASE)) return config.getLong(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getLong(path);
        }
        
        public boolean getBoolean(String path) {
            if (file.equals(Files.DATABASE)) return config.getBoolean(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getBoolean(path);
        }
        
        public List<String> getStringList(String path) {
            if (file.equals(Files.DATABASE)) return config.getStringList(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getStringList(path);
        }
        
        public ItemStack getItemStack(String path) {
            if (file.equals(Files.DATABASE)) return config.getItemStack(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getItemStack(path);
        }
        
        public ConfigurationSection getConfigurationSection(String path) {
            if (file.equals(Files.DATABASE)) return config.getConfigurationSection(path);
            if (config.get(path) == null) {
                reset(path);
            }
            return config.getConfigurationSection(path);
        }
        
        public boolean contains(String path) {
            return config.contains(path);
        }
        
        public void set(String path, Object obj) {
            config.set(path, obj);
        }
        
        protected void reset(String path) {
            if (defaultConfig.get(file) == null) {
                loadDefaultConfigurations();
            } else if (file.equals(Files.DATABASE)) {
                return;
            }
            FileConfiguration defaultFile = defaultConfig.get(file);
            config.set(path, defaultFile.get(path) != null ? defaultFile.get(path) : "Null");
            getInstance().saveFile(file);
        }
        
        protected void loadDefaultConfigurations() {
            String lang = Locale.getDefault().toString();
            String jarPath = "English";
            if (lang.equalsIgnoreCase("zh_cn")) {
                jarPath = "Chinese";
            }
            String fileName = file.getFileName();
            if (file.equals(Files.CONFIG)) {
                if (Version.getCurrentVersion().isOlder(Version.v1_13_R2)) {
                    fileName = "Config1.12.2-Down.yml";
                } else {
                    fileName = "Config1.13-Up.yml";
                }
            }
            InputStream resource = Main.getInstance().getResource("Languages/" + jarPath + "/" + fileName);
            if (resource != null) {
                try (InputStream input = resource;
                     Reader Config = new InputStreamReader(input, StandardCharsets.UTF_8)
                ) {
                    FileConfiguration configFile = new YamlConfiguration();
                    configFile.load(Config);
                    defaultConfig.put(file, configFile);
                } catch (IOException | InvalidConfigurationException ex) {
                    PluginControl.printStackTrace(ex);
                }
            }
        }
    }
}