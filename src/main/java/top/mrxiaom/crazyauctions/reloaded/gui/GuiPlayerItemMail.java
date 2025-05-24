package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;
import top.mrxiaom.crazyauctions.reloaded.util.Category;
import top.mrxiaom.crazyauctions.reloaded.util.ItemMail;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

import java.text.SimpleDateFormat;
import java.util.*;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.*;

public class GuiPlayerItemMail extends AbstractGui {
    private final ShopType type;
    private final Category category;
    private final @Nullable UUID uuid;
    private final Map<Integer, MenuIcon> icons = new HashMap<>();
    int page;
    Storage playerData;
    public GuiPlayerItemMail(Player player, ShopType type, Category category, int page, @Nullable UUID uuid) {
        super(player);
        this.type = type;
        this.category = category;
        this.uuid = uuid;
        this.page = page;
    }

    @Override
    protected void createInventory() {
        List<MenuIcon> items = new ArrayList<>();
        if (uuid == null) {
            playerData = Storage.getPlayer(player);
        } else {
            playerData = Storage.getPlayer(uuid);
        }
        if (!playerData.getMailBox().isEmpty()) {
            for (ItemMail im : playerData.getMailBox()) {
                if (im.getItem() == null) continue;
                List<String> lore = new ArrayList<>();
                String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(im.getAddedTime()));
                String time = PluginControl.convertToTime(im.getFullTime(), im.isNeverExpire());
                for (String l : MessageUtil.getValueList("Item-Mail-Lore")) {
                    lore.add(l.replace("%addedtime%", addedTime)
                            .replace("%time%", time));
                }
                items.add(MenuIcon.icon(im.getUID(), PluginControl.addLore(im.getItem().clone(), lore)));
            }
        }
        int maxPage = PluginControl.getMaxPage(items);
        page = Math.min(page, maxPage);
        inventory = create(54, PluginControl.color(player, config.getString("Settings.Player-Items-Mail") + " #" + page));
        List<String> options = new ArrayList<>();
        options.add("Player-Items-Mail-Back");
        options.add("PreviousPage");
        options.add("Return");
        options.add("NextPage");
        options.add("WhatIsThis.Items-Mail");
        for (String o : options) {
            if (config.contains("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                if (!config.getBoolean("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                    continue;
                }
            }
            addStandardIcon(config, inventory, "Settings.GUISettings.OtherSettings." + o, o);
        }
        List<Integer> indexes = getInvIndexes(config, "Settings.GUISettings.OtherSettings.Mail-Slots");
        icons.clear();
        icons.putAll(getPage(inventory, items, indexes, page));
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            if (itemFlag.equals("Player-Items-Mail-Back")) {
                PluginControl.updateCacheData();
                playClick(player);
                run(() -> openShop(player, type, category, 1));
                return;
            }
            if (itemFlag.equals("PreviousPage")) {
                PluginControl.updateCacheData();
                this.page = Math.max(1, page - 1);
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("NextPage")) {
                PluginControl.updateCacheData();
                this.page++;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Return")) {
                PluginControl.updateCacheData();
                for (ItemMail im : playerData.getMailBox()) {
                    if (PluginControl.isInvFull(player)) {
                        MessageUtil.sendMessage(player, "Inventory-Full");
                        playerData.saveData();
                        return;
                    }
                    im.giveItem();
                }
                playerData.clearMailBox();
                MessageUtil.sendMessage(player, "Got-All-Item-Back");
                playClick(player);
                reopen();
                return;
            }
        }
        MenuIcon icon = icons.get(slot);
        if (icon != null) {
            long uid = icon.uid;
            for (ItemMail im : playerData.getMailBox()) {
                if (uid == im.getUID()) {
                    if (!PluginControl.isInvFull(player)) {
                        MessageUtil.sendMessage(player, "Got-Item-Back");
                        im.giveItem();
                        playerData.saveData();
                        playClick(player);
                        page = 1;
                        reopen();
                    } else {
                        MessageUtil.sendMessage(player, "Inventory-Full");
                    }
                    return;
                }
            }
            playClick(player);
            run(() -> openShop(player, type, category, 1));
            MessageUtil.sendMessage(player, "Item-Doesnt-Exist");
        }
    }
}
