package top.mrxiaom.crazyauctions.reloaded.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;

/**
 *
 * @author BadBones69
 *
 * This event is fired when a player buys something from the selling auction house.
 *
 */
public class AuctionBuyEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final double price;
    private final MarketGoods mg;
    
    /**
     */
    public AuctionBuyEvent(Player player, MarketGoods mg, double price) {
        this.player = player;
        this.mg = mg;
        this.price = price;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public MarketGoods getMarketGoods() {
        return mg;
    }
    
    public double getPrice() {
        return price;
    }
    
}