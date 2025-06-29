package top.mrxiaom.crazyauctions.reloaded.database.engine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.database.DatabaseEngine;
import top.mrxiaom.crazyauctions.reloaded.database.StorageMethod;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager.*;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

public class MySQLEngine
    implements DatabaseEngine
{
    private static final MySQLEngine instance = new MySQLEngine();
    
    private static volatile Connection connection = null;
    private static String hostname = "localhost";
    private static String port = "3306";
    private static String username = "root";
    private static String password = "password";
    private static String database = "crazyauctionsreloaded";
    private static String parameter = "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
    private static String marketTable = "market";
    private static String itemMailTable = "itemMail";
    private static double updateDelay = 0;
    private static boolean marketReacquisition = false;
    private static boolean itemMailReacquisition = false;
    private static boolean databaseReloading = false;
    
    /**
     * Whether the returned data is empty.
     * @deprecated
     */
    @Deprecated
    protected boolean isEmpty(String sql) {
        try (ResultSet rs = executeQuery(sql)) {
            return rs.next();
        } catch (SQLException ex) {
            PluginControl.printStackTrace(ex);
            return false;
        }
    }
    
    /**
     * Whether the returned data is empty.
     * @deprecated
     */
    @Deprecated
    protected boolean isEmpty(PreparedStatement statement) {
        ResultSet rs = executeQuery(statement);
        try {
            return rs.next();
        } catch (SQLException ex) {
            PluginControl.printStackTrace(ex);
            return false;
        }
    }
    
    /**
     * Whether the returned data is empty.
     */
    protected boolean isEmpty(ResultSet rs) {
        try {
            return rs.next();
        } catch (SQLException ex) {
            PluginControl.printStackTrace(ex);
            return false;
        }
    }
    
    /**
     * Whether the database connection is MySQL-Reconnecting.
     */
    protected boolean isDatabaseReloading() {
        return databaseReloading;
    }
    
    public static MySQLEngine getInstance() {
        return instance;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void reloadConnectionParameters() {
        if (!PluginControl.useMySQLStorage()) return;
        // MySQL main parameters
        hostname = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Hostname");
        port = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Port");
        username = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Username");
        password = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Password");
        database = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Database");
        parameter = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Parameter");
        
        // Other settings
        itemMailTable = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Table-Name.Item-Mail");
        marketTable = Files.CONFIG.getFile().getString("Settings.MySQL-Storage.Table-Name.Market");
        updateDelay = Files.CONFIG.getFile().getDouble("Settings.MySQL-Storage.Data-Reacquisition.Delay");
        marketReacquisition = Files.CONFIG.getFile().getBoolean("Settings.MySQL-Storage.Data-Reacquisition.Market");
        itemMailReacquisition = Files.CONFIG.getFile().getBoolean("Settings.MySQL-Storage.Data-Reacquisition.Item-Mail");
        
        if (connection == null) {
            connectToTheDatabase();
        } else try {
            databaseReloading = true;
            if (Main.language.get("MySQL-Reconnect") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-Reconnect").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            Thread closing = new Thread(() -> {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException ex) {
                    PluginControl.printStackTrace(ex);
                }
            }, "Closing-Thread");
            closing.start();
            long time = System.currentTimeMillis();
            while (closing.isAlive()) {
                if (System.currentTimeMillis() - time > 10000) {
                    closing.stop();
                    break;
                }
                Thread.sleep(50);
            }
            connectToTheDatabase();
            databaseReloading = false;
        } catch (InterruptedException ex) {
            PluginControl.printStackTrace(ex);
        }
    }

    @Override
    public void connectToTheDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + parameter, username, password);
            if (Main.language.get("MySQL-SuccessfulConnection") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-SuccessfulConnection").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (PluginControl.useSplitDatabase()) {
                    if (PluginControl.getItemMailStorageMethod().equals(StorageMethod.MySQL)) {
                        createItemMailTable();
                    }
                    if (PluginControl.getMarketStorageMethod().equals(StorageMethod.MySQL)) {
                        createMarketTable();
                    }
                } else {
                    createItemMailTable();
                    createMarketTable();
                }
            } catch (SQLException ex) {
                if (Main.language.get("MySQL-DataTableCreationFailed") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataTableCreationFailed").replace("{prefix}", PluginControl.getPrefix()).replace("{error}", ex.getLocalizedMessage()).replace("&", "§"));
                Files.CONFIG.getFile().set("Settings.MySQL-Storage.Enabled", false);
                PluginControl.printStackTrace(ex);
            }
        } catch (ClassNotFoundException ex) {
            if (Main.language.get("MySQL-NoDriverFound") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-NoDriverFound").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            Files.CONFIG.getFile().set("Settings.MySQL-Storage.Enabled", false);
            PluginControl.printStackTrace(ex);
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-ConnectionError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-ConnectionError").replace("{prefix}", PluginControl.getPrefix()).replace("{error}", ex.getLocalizedMessage()).replace("&", "§"));
            Files.CONFIG.getFile().set("Settings.MySQL-Storage.Enabled", false);
            PluginControl.printStackTrace(ex);
        }
    }

    @Override
    public void repairConnection() {
        new Thread(() -> {
            int number = 0;
            while (true) {
                try {
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database + parameter, username, password);
                    if (Main.language.get("MySQL-ConnectionRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-ConnectionRepair").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                    break;
                } catch (SQLException ex) {
                    number++;
                    if (number == Files.CONFIG.getFile().getInt("Settings.MySQL-Storage.Automatic-Repair")) {
                        if (Main.language.get("MySQL-ConnectionRepairFailure") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-ConnectionRepairFailure").replace("{prefix}", PluginControl.getPrefix()).replace("{number}", String.valueOf(number)).replace("&", "§"));
                    } else {
                        if (Main.language.get("MySQL-BeyondRepair") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-BeyondRepair").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                        break;
                    }
                    PluginControl.printStackTrace(ex);
                }
            }
        }, "MySQLConnectionRepairThread").start();
    }

    @Override
    public void executeUpdate(PreparedStatement statement) {
        while (databaseReloading) {}
        try {
            while (isDatabaseNotExists()) {}
            statement.executeUpdate();
        }  catch (SQLException ex) {
            if (Main.language.get("MySQL-DataSavingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataSavingError").replace("{error}", ex.getLocalizedMessage()).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        }
}

    @Deprecated
    @Override
    public void executeUpdate(String sql) {
        while (databaseReloading) {}
        try {
            while (isDatabaseNotExists()) {}
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-DataSavingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataSavingError").replace("{error}", ex.getLocalizedMessage()).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        }
    }

    @Override
    public ResultSet executeQuery(PreparedStatement statement) {
        while (databaseReloading) {}
        try {
            while (isDatabaseNotExists()) {}
            return statement.executeQuery();
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-DataReadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataReadingError").replace("{error}", ex.getLocalizedMessage()).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        }
        return null;
    }

    @Deprecated
    @Override
    public ResultSet executeQuery(String sql) {
        while (databaseReloading) {}
        try {
            while (isDatabaseNotExists()) {}
            return connection.createStatement().executeQuery(sql);
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-DataReadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataReadingError").replace("{error}", ex.getLocalizedMessage()).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        }
        return null;
    }
    
    private void createItemMailTable() throws SQLException {
        connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + database + "; CREATE TABLE IF NOT EXISTS " + database + "." + itemMailTable + 
            "("
            + "UUID VARCHAR(36) NOT NULL PRIMARY KEY,"
            + "Name VARCHAR(16) NOT NULL,"
            + "YamlData LONGTEXT" +
            ");").executeUpdate();
    }
    
    private void createMarketTable() throws SQLException {
        connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + database + "; CREATE TABLE IF NOT EXISTS " + database + "." + marketTable + 
            "("
            + "YamlMarket LONGTEXT" +
            ");").executeUpdate();
    }
    
    private static boolean databaseExist = false;
    
    private boolean isDatabaseNotExists() throws SQLException {
        if (databaseExist) return false;
        ResultSet rs = connection.prepareStatement("SHOW DATABASES LIKE '" + database + "'").executeQuery();
        databaseExist = rs.next();
        return !databaseExist;
    }
    
    public static String getDatabaseName() {
        return database;
    }
    
    public static String getMarketTable() {
        return marketTable;
    }
    
    public static String getItemMailTable() {
        return itemMailTable;
    }
    
    public static double getUpdateDelay() {
        return updateDelay;
    }
    
    public static boolean isMarketReacquisition() {
        return marketReacquisition;
    }
    
    public static boolean isItemMailReacquisition() {
        return itemMailReacquisition;
    }
    
    /**
     * Back up all player data.
     * @param sqlConnection SQLite 's connection for backup files
     */
    public static void backupPlayerData(Connection sqlConnection) throws SQLException {
        ResultSet rs = instance.executeQuery(connection.prepareStatement("SELECT * FROM " + getDatabaseName() + "." + getItemMailTable()));
        while (rs.next()) {
            String name = rs.getString("Name");
            String uuid = rs.getString("UUID");
            String yaml = rs.getString("YamlData");
            PreparedStatement statement = sqlConnection.prepareStatement("INSERT INTO ItemMail (Name,UUID,YamlData) VALUES(?, ?, ?)");
            statement.setString(1, name);
            statement.setString(2, uuid);
            statement.setString(3, yaml);
            statement.executeUpdate();
        }
    }
}
