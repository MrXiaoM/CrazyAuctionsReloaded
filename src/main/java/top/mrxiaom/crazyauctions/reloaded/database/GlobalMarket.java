package top.mrxiaom.crazyauctions.reloaded.database;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;

import top.mrxiaom.crazyauctions.reloaded.database.market.MySQLMarket;
import top.mrxiaom.crazyauctions.reloaded.database.market.SQLiteMarket;
import top.mrxiaom.crazyauctions.reloaded.database.market.YamlMarket;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

public interface GlobalMarket
{
    /**
     * Get data for all products in the market
     */
    List<MarketGoods> getItems();

    /**
     * Get goods with uid.
     */
    MarketGoods getMarketGoods(long uid);
    
    /**
     * Adding new products to the market
     */
    void addGoods(MarketGoods goods);
    
    /**
     * Remove specified items from the market
     */
    void removeGoods(MarketGoods goods);
    
    /**
     * Remove item with specified UID
     */
    void removeGoods(long uid);

    /**
     * Remove specified items from the market cache
     */
    void removeGoodsFromCache(MarketGoods goods);

    /**
     * Clear global market.
     */
    void clearGlobalMarket();
    
    /**
     * Save market data
     */
    void saveData();
    
    /**
     * Reload market data from the database
     */
    void reloadData();
    
    /**
     * Make a new UID.
     */
    long makeUID();
    
    /**
     */
    YamlConfiguration getYamlData();
    
    static GlobalMarket getMarket() {
        if (PluginControl.useSplitDatabase()) {
            switch (PluginControl.getMarketStorageMethod()) {
                case MySQL: {
                    if (PluginControl.useMySQLStorage()) {
                        return MySQLMarket.getInstance();
                    } else {
                        return YamlMarket.getInstance();
                    }
                }
                case SQLite: {
                    if (PluginControl.useSQLiteStorage()) {
                        return SQLiteMarket.getInstance();
                    } else {
                        return YamlMarket.getInstance();
                    }
                }
                case YAML: {
                    return YamlMarket.getInstance();
                }
            }
        } else if (PluginControl.useMySQLStorage()) {
            return MySQLMarket.getInstance();
        } else if (PluginControl.useSQLiteStorage()) {
            return SQLiteMarket.getInstance();
        }
        return YamlMarket.getInstance();
    }
}
