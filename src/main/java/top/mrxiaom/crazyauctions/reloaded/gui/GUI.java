package top.mrxiaom.crazyauctions.reloaded.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.crazyauctions.reloaded.event.GuiManager;
import top.mrxiaom.crazyauctions.reloaded.util.*;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

public class GUI {
    public static final String ICON_KEY = "CrazyAuctionsReloaded_ICON";

    @Nullable
    @Contract("null -> null")
    public static String getFlag(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR)) return null;
        return NBT.get(item, nbt -> {
            if (nbt.hasTag(ICON_KEY, NBTType.NBTTagString)) {
                return nbt.getString(ICON_KEY);
            }
            return null;
        });
    }

    public static void putFlag(ItemStack item, @Nullable String value) {
        if (item == null || item.getType().equals(Material.AIR)) return;
        NBT.modify(item, nbt -> {
            if (value == null) {
                if (nbt.hasTag(ICON_KEY))
                    nbt.removeKey(ICON_KEY);
            } else {
                nbt.setString(ICON_KEY, value);
            }
        });
    }

    @NotNull
    public static ItemStack makeStandardIcon(FileManager.ProtectedConfiguration config, String prefix, String flag) {
        return makeStandardIcon(config, prefix, flag, null);
    }
    @NotNull
    public static ItemStack makeStandardIcon(FileManager.ProtectedConfiguration config, String prefix, String flag, @Nullable Function<List<String>, List<String>> loreModifier) {
        String id = config.getString(prefix + ".Item");
        String customModelKey = prefix + ".CustomModelData";
        Integer customModel = config.contains(customModelKey) ? config.getInt(customModelKey) : null;
        String name = config.getString(prefix + ".Name");
        List<String> lore;
        if (config.contains(prefix + ".Lore")) {
            List<String> list = config.getStringList(prefix + ".Lore");
            lore = loreModifier == null ? list : loreModifier.apply(list);
        } else {
            lore = null;
        }
        ItemStack item = lore == null
                ? PluginControl.makeItem(id, 1, name)
                : PluginControl.makeItem(id, 1, name, lore);
        putFlag(item, flag);
        if (customModel != null) {
            AdventureItemStack.setCustomModelData(item, customModel);
        }
        return item;
    }

    public static void addStandardIcon(FileManager.ProtectedConfiguration config, Inventory inv, String prefix, String flag) {
        addStandardIcon(config, inv, prefix, flag, null);
    }
    public static void addStandardIcon(FileManager.ProtectedConfiguration config, Inventory inv, String prefix, String flag, @Nullable Function<List<String>, List<String>> loreModifier) {
        ItemStack item = makeStandardIcon(config, prefix, flag, loreModifier);
        List<Integer> slots = config.getConfig().getIntegerList(prefix + ".Slots");
        if (config.contains(prefix + ".Slot")) {
            slots.add(config.getInt(prefix + ".Slot"));
        }
        for (int slot : slots) {
            inv.setItem(slot - 1, item);
        }
    }

    public static List<Integer> getInvIndexes(FileManager.ProtectedConfiguration section, String key) {
        List<Integer> list = new ArrayList<>();
        char[] array = String.join("", section.getStringList(key)).toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (array[i] == '#') {
                list.add(i);
            }
        }
        return list;
    }

    public static Map<Integer, MenuIcon> getPage(Inventory inv, List<MenuIcon> items, List<Integer> indexes, int page) {
        Map<Integer, MenuIcon> ids = new HashMap<>();
        List<MenuIcon> iconList = PluginControl.getPage(items, page, indexes.size());
        int pageLength = Math.min(indexes.size(), iconList.size());
        for (int i = 0; i < pageLength; i++) {
            int slot = indexes.get(i);
            MenuIcon icon = iconList.get(i);
            inv.setItem(slot, icon.item);
            ids.put(slot, icon);
        }
        return ids;
    }

    /**
     * 打开全球市场菜单 /ca gui
     */
    public static void openShop(Player player, ShopType type, Category cat, int page) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiShop(player, type, cat, page).open();
    }

    /**
     * 打开全球市场分类菜单 /ca gui
     */
    public static void openCategories(Player player, ShopType shop, Category category) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiCategories(player, shop, category).open();
    }

    /**
     * 打开玩家物品列表菜单 /ca listed
     */
    public static void openPlayersCurrentList(Player player, ShopType type, Category category, int page) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiPlayerItemList(player, type, category, page).open();
    }

    /**
     * 打开玩家收件箱 /ca mail
     */
    public static void openPlayersMail(Player player, ShopType type, Category category, int page) {
        openPlayersMail(player, type, category, page, null);
    }

    /**
     * 管理员打开玩家收件箱 /ca admin player view
     */
    public static void openPlayersMail(Player player, ShopType type, Category category, int page, @Nullable UUID uuid) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiPlayerItemMail(player, type, category, page, uuid).open();
    }

    /**
     * 在全球市场页面，打开购买菜单
     */
    public static void openBuying(Player player, ShopType type, Category category, MarketGoods mg) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiItemBuying(player, type, category, mg).open();
    }

    /**
     * 在全球市场页面，打开出售菜单
     */
    public static void openSelling(Player player, ShopType type, Category category, MarketGoods mg) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiItemSelling(player, type, category, mg).open();
    }

    /**
     * 在全球市场页面，打开竞拍菜单
     */
    public static void openBidding(Player player, MarketGoods mg) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiItemBidding(player, mg).open();
    }

    /**
     * 浏览玩家商店 /ca view
     */
    public static void openViewer(Player player, UUID uuid, int page) {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        new GuiItemViewer(player, uuid, page).open();
    }
    
    public static void playClick(Player player) {
        if (FileManager.Files.CONFIG.getFile().contains("Settings.Sounds.Toggle")) {
            if (FileManager.Files.CONFIG.getFile().getBoolean("Settings.Sounds.Toggle")) {
                String sound = FileManager.Files.CONFIG.getFile().getString("Settings.Sounds.Sound");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
                } catch (Exception e) {
                    PluginControl.printStackTrace(e);
                }
            }
        } else {
            if (PluginControl.getVersion() >= 191) {
                player.playSound(player.getLocation(), Sound.valueOf("UI_BUTTON_CLICK"), 1, 1);
            } else {
                player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 1, 1);
            }
        }
    }

    public static void closeAllGui() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (GuiManager.inst().getOpeningGui(player) != null) {
                player.closeInventory();
            }
        }
    }
}
