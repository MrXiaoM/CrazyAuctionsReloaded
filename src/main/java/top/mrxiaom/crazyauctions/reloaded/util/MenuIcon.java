package top.mrxiaom.crazyauctions.reloaded.util;

import org.bukkit.inventory.ItemStack;

import static top.mrxiaom.crazyauctions.reloaded.util.GUI.putFlag;

public class MenuIcon {
    public final long uid;
    public final ItemStack item;

    public MenuIcon(long uid, ItemStack item) {
        this.uid = uid;
        this.item = item;
        putFlag(item, null);
    }

    public static MenuIcon icon(long uid, ItemStack item) {
        return new MenuIcon(uid, item);
    }
}
