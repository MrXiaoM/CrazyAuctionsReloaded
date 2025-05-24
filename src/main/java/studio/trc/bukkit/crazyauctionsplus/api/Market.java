package studio.trc.bukkit.crazyauctionsplus.api;

import studio.trc.bukkit.crazyauctionsplus.database.GlobalMarket;

/**
 * This is just a handy guide.
 */
public class Market
{
    public static GlobalMarket getGlobalMarket() {
        return GlobalMarket.getMarket();
    }
}
