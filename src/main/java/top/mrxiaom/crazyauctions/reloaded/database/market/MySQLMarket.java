package top.mrxiaom.crazyauctions.reloaded.database.market;

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

import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.engine.MySQLEngine;
import top.mrxiaom.crazyauctions.reloaded.data.ItemOwner;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;

public class MySQLMarket
    extends MySQLEngine
    implements GlobalMarket
{
    private static final List<MarketGoods> marketGoods = new ArrayList<>();
    
    private static MySQLMarket instance;
    private static long lastUpdateTime = System.currentTimeMillis();
    
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
            YamlConfiguration data = new YamlConfiguration();
            for (MarketGoods mg : marketGoods) {
                long num = 1;
                while (data.contains("Items." + num)) num++;
                data.set("Items." + num + ".Owner", mg.getItemOwner().toString());
                switch (mg.getShopType()) {
                    case SELL: {
                        data.set("Items." + num + ".Price", mg.getPrice());
                        data.set("Items." + num + ".ShopType", "SELL");
                        data.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        data.set("Items." + num + ".Full-Time", mg.getFullTime());
                        data.set("Items." + num + ".UID", mg.getUID());
                        data.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                    case BUY: {
                        data.set("Items." + num + ".Reward", mg.getReward());
                        data.set("Items." + num + ".ShopType", "BUY");
                        data.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        data.set("Items." + num + ".Full-Time", mg.getFullTime());
                        data.set("Items." + num + ".UID", mg.getUID());
                        data.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                    case BID: {
                        data.set("Items." + num + ".Price", mg.getPrice());
                        data.set("Items." + num + ".ShopType", "BID");
                        data.set("Items." + num + ".TopBidder", mg.getTopBidder());
                        data.set("Items." + num + ".Time-Till-Expire", mg.getTimeTillExpire());
                        data.set("Items." + num + ".Full-Time", mg.getFullTime());
                        data.set("Items." + num + ".UID", mg.getUID());
                        data.set("Items." + num + ".Item", mg.getItem());
                        break;
                    }
                }
            }
            PreparedStatement statement = getConnection().prepareStatement("UPDATE " + getDatabaseName() + "." + getMarketTable() + " SET "
                    + "YamlMarket = ?");
            statement.setString(1, data.saveToString());
            executeUpdate(statement);
            lastValidData = data;
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
                YamlConfiguration data = new YamlConfiguration();
                String stringYaml = rs.getString("YamlMarket");
                if (stringYaml.isEmpty()) {
                    return;
                } else {
                    data.loadFromString(stringYaml);
                }
                ConfigurationSection section = data.getConfigurationSection("Items");
                if (section != null) for (String path : Lists.newArrayList(section.getKeys(false))) {
                    String[] owner = section.getString(path + ".Owner", "").split(":");
                    String typeString = section.getString(path + ".ShopType", "");
                    if (owner.length != 2 || typeString.isEmpty()) {
                        YamlConfiguration save = new YamlConfiguration();
                        save.set(path, section.getConfigurationSection(path));
                        Main.getInstance().getLogger().warning("Skipped invalid market item:\n" + save.saveToString());
                        continue;
                    }
                    ShopType shopType = ShopType.valueOf(typeString.toUpperCase());
                    MarketGoods goods;
                    switch (shopType) {
                        case SELL: {
                            goods = new MarketGoods(
                                    section.getLong(path + ".UID"),
                                    shopType,
                                    new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                    section.getItemStack(path + ".Item"),
                                    section.getLong(path + ".Time-Till-Expire"),
                                    section.getLong(path + ".Full-Time"),
                                    section.get(path + ".Added-Time") != null ? section.getLong(path + ".Added-Time") : -1,
                                    section.getDouble(path + ".Price")
                            );
                            break;
                        }
                        case BUY: {
                            goods = new MarketGoods(
                                    section.getLong(path + ".UID"),
                                    shopType,
                                    new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                    section.getItemStack(path + ".Item"),
                                    section.getLong(path + ".Time-Till-Expire"),
                                    section.getLong(path + ".Full-Time"),
                                    section.get(path + ".Added-Time") != null ? data.getLong(path + ".Added-Time") : -1,
                                    section.getDouble(path + ".Reward")
                            );
                            break;
                        }
                        case BID: {
                            goods = new MarketGoods(
                                    section.getLong(path + ".UID"),
                                    shopType,
                                    new ItemOwner(UUID.fromString(owner[1]), owner[0]),
                                    section.getItemStack(path + ".Item"),
                                    section.getLong(path + ".Time-Till-Expire"),
                                    section.getLong(path + ".Full-Time"),
                                    section.get(path + ".Added-Time") != null ? section.getLong(path + ".Added-Time") : -1,
                                    section.getDouble(path + ".Price"),
                                    section.getString(path + ".TopBidder")
                            );
                            break;
                        }
                        default: {
                            continue;
                        }
                    }
                    marketGoods.add(goods);
                }
                lastValidData = data;
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

    private YamlConfiguration lastValidData = new YamlConfiguration();
    @Override
    public YamlConfiguration getYamlData() {
        return lastValidData;
    }
}
