package top.mrxiaom.crazyauctions.reloaded.util;

import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CrazyAuctions {
    
    private static final CrazyAuctions instance = new CrazyAuctions();
    
    public static CrazyAuctions getInstance() {
        return instance;
    }
    
    public Boolean isSellingEnabled() {
        return FileManager.Files.CONFIG.getFile().getBoolean("Settings.Feature-Toggle.Selling");
    }
    
    public Boolean isBiddingEnabled() {
        return FileManager.Files.CONFIG.getFile().getBoolean("Settings.Feature-Toggle.Bidding");
    }
    
    public Boolean isBuyingEnabled() {
        return FileManager.Files.CONFIG.getFile().getBoolean("Settings.Feature-Toggle.Buying");
    }
    
    public int getNumberOfPlayerItems(Player player, ShopType type) {
        int number = 0;
        GlobalMarket market = GlobalMarket.getMarket();
        if (market.getItems().isEmpty()) return number;
        switch (type) {
            case SELL: {
                for (MarketGoods mg : market.getItems()) {
                    if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                        if (mg.getShopType().equals(ShopType.SELL)) {
                            number++;
                        }
                    }
                }
                return number;
            }
            case BUY: {
                for (MarketGoods mg : market.getItems()) {
                    if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                        if (mg.getShopType().equals(ShopType.BUY)) {
                            number++;
                        }
                    }
                }
                return number;
            }
            case BID: {
                for (MarketGoods mg : market.getItems()) {
                    if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                        if (mg.getShopType().equals(ShopType.BID)) {
                            number++;
                        }
                    }
                }
                return number;
            }
        }
        return number;
    }
    
    public List<MarketGoods> getMarketItems(Player player) {
        List<MarketGoods> items = new ArrayList<>();
        GlobalMarket market = GlobalMarket.getMarket();
        if (!market.getItems().isEmpty()) {
            for (MarketGoods mg : market.getItems()) {
                if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                    items.add(mg);
                }
            }
        }
        return items;
    }
}