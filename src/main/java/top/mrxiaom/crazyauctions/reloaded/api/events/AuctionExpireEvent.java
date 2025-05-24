package top.mrxiaom.crazyauctions.reloaded.api.events;

import org.jetbrains.annotations.NotNull;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionExpireEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private OfflinePlayer offlinePlayer;
    private Player onlinePlayer;
    private final boolean isOnline;
    private final MarketGoods mg;
    private final ShopType type;
    
    public AuctionExpireEvent(OfflinePlayer offlinePlayer, MarketGoods mg, ShopType type) {
        this.offlinePlayer = offlinePlayer;
        this.mg = mg;
        this.isOnline = false;
        this.type = type;
    }
    
    public AuctionExpireEvent(Player onlinePlayer, MarketGoods mg, ShopType type) {
        this.onlinePlayer = onlinePlayer;
        this.mg = mg;
        this.isOnline = true;
        this.type = type;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }
    
    public Player getOnlinePlayer() {
        return onlinePlayer;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public MarketGoods getMarketGoods() {
        return mg;
    }
    
    public ShopType getShopType() {
        return type;
    }
}