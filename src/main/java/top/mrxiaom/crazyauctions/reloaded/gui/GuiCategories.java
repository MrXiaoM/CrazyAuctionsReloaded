package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.data.Category;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;

import java.util.ArrayList;
import java.util.List;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.*;

public class GuiCategories extends AbstractGui {
    private ShopType shop;
    private final Category category;
    public GuiCategories(Player player, ShopType shop, Category category) {
        super(player);
        this.shop = shop;
        this.category = category;
    }

    @Override
    protected void createInventory() {
        int size = config.getInt("Settings.GUISettings.Category-Settings.GUI-Size");
        if (size != 54 && size != 45 && size != 36 && size != 27 && size != 18 && size != 9) {
            size = 54;
        }
        inventory = create(size, PluginControl.color(player, config.getString("Settings.Categories")));
        List<String> options = new ArrayList<>();
        options.add("OtherSettings.Categories-Back");
        options.add("OtherSettings.WhatIsThis.Categories");
        for (String option : config.getConfigurationSection("Settings.GUISettings.Category-Settings.Custom-Category").getKeys(false)) {
            options.add("Category-Settings.Custom-Category." + option);
        }
        options.add("Category-Settings.ShopType-Category.Selling");
        options.add("Category-Settings.ShopType-Category.Buying");
        options.add("Category-Settings.ShopType-Category.Bidding");
        options.add("Category-Settings.ShopType-Category.None");
        for (String o : options) {
            if (config.contains("Settings.GUISettings." + o + ".Toggle")) {
                if (!config.getBoolean("Settings.GUISettings." + o + ".Toggle")) {
                    continue;
                }
            }
            addStandardIcon(config, inventory, "Settings.GUISettings." + o, o);
        }
        player.openInventory(inventory);
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            for (String name : config.getConfigurationSection("Settings.GUISettings.Category-Settings.Custom-Category").getKeys(false)) {
                Category category = Category.getModule(config.getString("Settings.GUISettings.Category-Settings.Custom-Category." + name + ".Category-Module"));
                if (category == null) continue;
                if (itemFlag.equals("Category-Settings.Custom-Category." + name)) {
                    run(() -> openShop(player, shop, category, 1));
                    playClick(player);
                    return;
                }
            }
            if (itemFlag.equals("OtherSettings.Categories-Back")) {
                run(() -> openShop(player, shop, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Category-Settings.ShopType-Category.Selling")) {
                run(() -> openShop(player, shop = ShopType.SELL, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Category-Settings.ShopType-Category.Buying")) {
                run(() -> openShop(player, shop = ShopType.BUY, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Category-Settings.ShopType-Category.Bidding")) {
                run(() -> openShop(player, shop = ShopType.BID, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Category-Settings.ShopType-Category.None")) {
                run(() -> openShop(player, shop = ShopType.ANY, category, 1));
                playClick(player);
                return;
            }
        }
    }
}
