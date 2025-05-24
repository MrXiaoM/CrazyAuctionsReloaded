package top.mrxiaom.crazyauctions.reloaded.api;

import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;

/**
 * This is just a handy guide.
 */
public class Market
{
    public static GlobalMarket getGlobalMarket() {
        return GlobalMarket.getMarket();
    }
}
