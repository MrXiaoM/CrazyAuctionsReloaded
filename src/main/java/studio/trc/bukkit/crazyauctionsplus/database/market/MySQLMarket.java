package studio.trc.bukkit.crazyauctionsplus.database.market;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import studio.trc.bukkit.crazyauctionsplus.Main;
import studio.trc.bukkit.crazyauctionsplus.database.GlobalMarket;
import studio.trc.bukkit.crazyauctionsplus.database.engine.MySQLEngine;
import studio.trc.bukkit.crazyauctionsplus.util.ItemOwner;
import studio.trc.bukkit.crazyauctionsplus.util.MarketGoods;
import studio.trc.bukkit.crazyauctionsplus.util.PluginControl;
import studio.trc.bukkit.crazyauctionsplus.util.enums.ShopType;

public class MySQLMarket
    extends MySQLEngine
    implements GlobalMarket
{
    private static final List<MarketGoods> marketGoods = new ArrayList<>();
    
    private static MySQLMarket instance;
    private static long lastUpdateTime = System.currentTimeMillis();
    
    private final YamlConfiguration yamlMarket = new YamlConfiguration();
    
    private MySQLMarket() {
        instance = MySQLMarket.this;
    }
    
    public static MySQLMarket getInstance() {
        if (instance == null) {
            return new MySQLMarket();
        }
        return instance;
    }

    @Override
    public List<MarketGoods> getItems() {
        if (getUpdateDelay() == 0) {
            reloadData();
        } else if (isMarketReacquisition() && System.currentTimeMillis() - lastUpdateTime >= getUpdateDelay() * 1000) { 
            reloadData();
            lastUpdateTime = System.currentTimeMillis();
        }
        return Collections.unmodifiableList(Lists.newArrayList(marketGoods));
    }
    
    @Override
    public MarketGoods getMarketGoods(long uid) {
        if (getUpdateDelay() == 0) {
            reloadData();
        } else if (isMarketReacquisition() && System.currentTimeMillis() - lastUpdateTime >= getUpdateDelay() * 1000) { 
            reloadData();
            lastUpdateTime = System.currentTimeMillis();
        }
        for (MarketGoods mg : marketGoods) {
            if (mg.getUID() == uid) {
                return mg;
            }
        }
        return null;
    }
    
    @Override
    public long makeUID() {
        long id = 0;
        while (true) {
            id++;
            boolean b = false;
            if (getUpdateDelay() == 0) {
                reloadData();
            } else if (isMarketReacquisition() && System.currentTimeMillis() - lastUpdateTime >= getUpdateDelay() * 1000) { 
                reloadData();
                lastUpdateTime = System.currentTimeMillis();
            }
            for (MarketGoods mgs : marketGoods) {
                if (mgs.getUID() == id) {
                    b = true;
                    break;
                }
            }
            if (b) continue;
            break;
        }
        return id;
    }

    @Override
    public void addGoods(MarketGoods goods) {
        marketGoods.add(goods);
        saveData();
    }

    @Override
    public void removeGoods(MarketGoods goods) {
        if (getUpdateDelay() == 0) {
            reloadData();
        } else if (isMarketReacquisition() && System.currentTimeMillis() - lastUpdateTime >= getUpdateDelay() * 1000) { 
            reloadData();
            lastUpdateTime = System.currentTimeMillis();
        }
        removeGoodsFromCache(goods);
        saveData();
    }
    
    @Override
    public void removeGoods(long uid) {
        if (getUpdateDelay() == 0) {
            reloadData();
        } else if (isMarketReacquisition() && System.currentTimeMillis() - lastUpdateTime >= getUpdateDelay() * 1000) { 
            reloadData();
            lastUpdateTime = System.currentTimeMillis();
        }
        for (MarketGoods mg : marketGoods) {
            if (mg.getUID() == uid) {
                marketGoods.remove(mg);
                break;
            }
        }
        saveData();
    }

    @Override
    public void removeGoodsFromCache(MarketGoods goods) {
        marketGoods.remove(goods);
    }

    @Override
    public void clearGlobalMarket() {
        marketGoods.clear();
        saveData();
    }

    @Override
    public void saveData() {
        try {
            yamlMarket.set("Items", null);
            for (MarketGoods mg : marketGoods) {
                long num = 1;
                while (yamlMarket.contains("Items." + num)) num++;
                yamlMarket.set("Items." + num + ".Owner", mg.getItemOwner().toString());
                switch (mg.getShopType()) {
                    case SELL: {
                        yamlMarket.set("Items." + num + ".Price", mg.getPrice());
                        yamlMarket.set("Items." + num + ".ShopType", "SELL");
                        yamlMarket.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        yamlMarket.set("Items." + num + ".Full-Time", mg.getFullTime());
                        yamlMarket.set("Items." + num + ".UID", mg.getUID());
                        yamlMarket.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                    case BUY: {
                        yamlMarket.set("Items." + num + ".Reward", mg.getReward());
                        yamlMarket.set("Items." + num + ".ShopType", "BUY");
                        yamlMarket.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        yamlMarket.set("Items." + num + ".Full-Time", mg.getFullTime());
                        yamlMarket.set("Items." + num + ".UID", mg.getUID());
                        yamlMarket.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                    case BID: {
                        yamlMarket.set("Items." + num + ".Price", mg.getPrice());
                        yamlMarket.set("Items." + num + ".ShopType", "BID");
                        yamlMarket.set("Items." + num + ".TopBidder", mg.getTopBidder());
                        yamlMarket.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        yamlMarket.set("Items." + num + ".Full-Time", mg.getFullTime());
                        yamlMarket.set("Items." + num + ".UID", mg.getUID());
                        yamlMarket.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                }
            }
            PreparedStatement statement = getConnection().prepareStatement("UPDATE " + getDatabaseName() + "." + getMarketTable() + " SET "
                    + "YamlMarket = ?");
            statement.setString(1, yamlMarket.saveToString());
            executeUpdate(statement);
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-DataSavingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataSavingError").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) repairConnection();
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        }
    }
    
    @Override
    public void reloadData() {
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM " + getDatabaseName() + "." + getMarketTable());
            ResultSet rs = executeQuery(statement);
            marketGoods.clear();
            if (rs != null && rs.next()) {
                String stringYaml = rs.getString("YamlMarket");
                if (stringYaml.isEmpty()) {
                    return;
                } else {
                    yamlMarket.loadFromString(stringYaml);
                }
                if (yamlMarket.get("Items") == null) return;
                ConfigurationSection section = yamlMarket.getConfigurationSection("Items");
                if (section != null) for (String path : section.getKeys(false)) {
                    String[] owner = yamlMarket.getString("Items." + path + ".Owner", "").split(":");
                    ShopType shoptype = ShopType.valueOf(yamlMarket.getString("Items." + path + ".ShopType", "").toUpperCase());
                    MarketGoods goods;
                    switch (shoptype) {
                        case SELL: {
                            goods = new MarketGoods(
                                yamlMarket.getLong("Items." + path + ".UID"),
                                shoptype,
                                new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                yamlMarket.getItemStack("Items." + path + ".Item"),
                                yamlMarket.getLong("Items." + path + ".Time-Till-Expire"),
                                yamlMarket.getLong("Items." + path + ".Full-Time"),
                                yamlMarket.get("Items." + path + ".Added-Time") != null ? yamlMarket.getLong("Items." + path + ".Added-Time") : -1,
                                yamlMarket.getDouble("Items." + path + ".Price")
                            );
                            break;
                        }
                        case BUY: {
                            goods = new MarketGoods(
                                yamlMarket.getLong("Items." + path + ".UID"),
                                shoptype,
                                new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                yamlMarket.getItemStack("Items." + path + ".Item"),
                                yamlMarket.getLong("Items." + path + ".Time-Till-Expire"),
                                yamlMarket.getLong("Items." + path + ".Full-Time"),
                                yamlMarket.get("Items." + path + ".Added-Time") != null ? yamlMarket.getLong("Items." + path + ".Added-Time") : -1,
                                yamlMarket.getDouble("Items." + path + ".Reward")
                            );
                            break;
                        }
                        case BID: {
                            goods = new MarketGoods(
                                yamlMarket.getLong("Items." + path + ".UID"),
                                shoptype,
                                new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                yamlMarket.getItemStack("Items." + path + ".Item"),
                                yamlMarket.getLong("Items." + path + ".Time-Till-Expire"),
                                yamlMarket.getLong("Items." + path + ".Full-Time"),
                                yamlMarket.get("Items." + path + ".Added-Time") != null ? yamlMarket.getLong("Items." + path + ".Added-Time") : -1,
                                yamlMarket.getDouble("Items." + path + ".Price"),
                                yamlMarket.getString("Items." + path + ".TopBidder")
                            );
                            break;
                        }
                        default: {
                            continue;
                        }
                    }
                    marketGoods.add(goods);
                }
            } else {
                PreparedStatement createMarket = getConnection().prepareStatement("INSERT INTO " + getDatabaseName() + "." + getMarketTable() + " (YamlMarket) VALUES(?)");
                createMarket.setString(1, "{}");
                executeUpdate(createMarket);
            }
        } catch (SQLException ex) {
            if (Main.language.get("MySQL-DataReadingError") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MySQL-DataReadingError").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            try {
                if (getConnection().isClosed()) {
                    repairConnection();
                    reloadData();
                }
            } catch (SQLException ex1) {
                PluginControl.printStackTrace(ex1);
            }
            PluginControl.printStackTrace(ex);
        } catch (InvalidConfigurationException | NullPointerException ex) {
            if (Main.language.get("MarketDataFailedToLoad") != null) Main.getInstance().getServer().getConsoleSender().sendMessage(Main.language.getProperty("MarketDataFailedToLoad").replace("{error}", ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "null").replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
            PluginControl.printStackTrace(ex);
        }
    }
    
    @Override
    public YamlConfiguration getYamlData() {
        return yamlMarket;
    }
}
