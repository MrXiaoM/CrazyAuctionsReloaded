package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionCancelledEvent;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.data.Category;
import top.mrxiaom.crazyauctions.reloaded.data.ItemMail;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;
import top.mrxiaom.crazyauctions.reloaded.util.*;
import top.mrxiaom.crazyauctions.reloaded.api.events.CancelledReason;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;

import java.text.SimpleDateFormat;
import java.util.*;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.*;

public class GuiShop extends AbstractGui {
    private final Category category;
    private final Map<Integer, MenuIcon> icons = new HashMap<>();
    private ShopType type;
    private int page;
    public GuiShop(Player player, ShopType type, Category category, int page) {
        super(player);
        this.type = type;
        this.category = category;
        this.page = page;
    }
    @Override
    public void createInventory() {
        List<MenuIcon> items = new ArrayList<>();
        GlobalMarket market = GlobalMarket.getMarket();
        for (MarketGoods mg : market.getItems()) {
            List<String> lore = new ArrayList<>();
            if ((category.isWhitelist() == category.getAllItemMeta().contains(mg.getItem().getItemMeta()))
                    || category.getItems().contains(mg.getItem().getType())
                    || category.equals(Category.getDefaultCategory())) {
                switch (type) {
                    case BID: {
                        if (mg.getShopType().equals(ShopType.BID)) {
                            String owner = mg.getItemOwner().getName();
                            String topBidder = mg.getTopBidder().split(":")[0];
                            for (String l : MessageUtil.getValueList("BiddingItemLore")) {
                                String price = String.valueOf(mg.getPrice());
                                String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                lore.add(l.replace("%topbid%", price)
                                        .replace("%owner%", owner)
                                        .replace("%addedtime%", addedTime)
                                        .replace("%topbidder%", topBidder)
                                        .replace("%time%", time));
                            }

                            if (mg.getItem() == null) continue;
                            items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                        }
                        break;
                    }
                    case BUY: {
                        if (mg.getShopType().equals(ShopType.BUY)) {
                            for (String l : MessageUtil.getValueList("BuyingItemLore")) {
                                String reward = String.valueOf(mg.getReward());
                                String owner = mg.getItemOwner().getName();
                                String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                lore.add(l.replace("%reward%", reward)
                                        .replace("%owner%", owner)
                                        .replace("%addedtime%", addedTime)
                                        .replace("%time%", time));
                            }
                            items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                        }
                        break;
                    }
                    case SELL: {
                        if (mg.getShopType().equals(ShopType.SELL)) {
                            for (String l : MessageUtil.getValueList("SellingItemLore")) {
                                String price = String.valueOf(mg.getPrice());
                                String owner = mg.getItemOwner().getName();
                                String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                lore.add(l.replace("%price%", price)
                                        .replace("%Owner%", owner)
                                        .replace("%owner%", owner)
                                        .replace("%addedtime%", addedTime)
                                        .replace("%time%", time));
                            }
                            if (mg.getItem() == null) continue;
                            items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                        }
                        break;
                    }
                    case ANY: {
                        switch (mg.getShopType()) {
                            case BID: {
                                String owner = mg.getItemOwner().getName();
                                String topbidder = mg.getTopBidder().split(":")[0];
                                for (String l : MessageUtil.getValueList("BiddingItemLore")) {
                                    String price = String.valueOf(mg.getPrice());
                                    String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                    String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                    lore.add(l.replace("%topbid%", price)
                                            .replace("%addedtime%", addedTime)
                                            .replace("%owner%", owner)
                                            .replace("%topbidder%", topbidder)
                                            .replace("%time%", time));
                                }
                                if (mg.getItem() == null) continue;
                                items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                                break;
                            }
                            case BUY: {
                                for (String l : MessageUtil.getValueList("BuyingItemLore")) {
                                    String reward = String.valueOf(mg.getReward());
                                    String owner = mg.getItemOwner().getName();
                                    String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                    String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                    lore.add(l.replace("%reward%", reward)
                                            .replace("%Owner%", owner)
                                            .replace("%owner%", owner)
                                            .replace("%addedtime%", addedTime)
                                            .replace("%time%", time));
                                }
                                items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                                break;
                            }
                            case SELL: {
                                for (String l : MessageUtil.getValueList("SellingItemLore")) {
                                    String price = String.valueOf(mg.getPrice());
                                    String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                                    String owner = mg.getItemOwner().getName();
                                    String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                                    lore.add(l.replace("%price%", price)
                                            .replace("%addedtime%", addedTime)
                                            .replace("%owner%", owner)
                                            .replace("%time%", time));
                                }
                                if (mg.getItem() == null) continue;
                                items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                                break;
                            }
                        }
                    }
                }
            }
        }
        int maxPage = PluginControl.getMaxPage(items);
        while (page > maxPage) page--;
        if (type == null) {
            type = ShopType.ANY;
        }
        switch (type) {
            case ANY: {
                inventory = create(54, PluginControl.color(player, config.getString("Settings.Main-GUIName") + " #" + page));
                break;
            }
            case SELL: {
                inventory = create(54, PluginControl.color(player, config.getString("Settings.Sell-GUIName") + " #" + page));
                break;
            }
            case BUY: {
                inventory = create(54, PluginControl.color(player, config.getString("Settings.Buy-GUIName") + " #" + page));
                break;
            }
            case BID: {
                inventory = create(54, PluginControl.color(player, config.getString("Settings.Bid-GUIName") + " #" + page));
                break;
            }
            default: {
                throw new NullPointerException("ShopType is null");
            }
        }
        List<String> options = new ArrayList<>();
        options.add("Commoditys");
        options.add("Items-Mail");
        options.add("PreviousPage");
        options.add("Refesh");
        options.add("NextPage");
        options.add("Category");
        options.add("Custom");
        switch (type) {
            case SELL: {
                if (CrazyAuctions.getInstance().isSellingEnabled()) {
                    options.add("Shopping.Selling");
                }
                options.add("WhatIsThis.SellingShop");
                break;
            }
            case BID: {
                if (CrazyAuctions.getInstance().isBiddingEnabled()) {
                    options.add("Shopping.Bidding");
                }
                options.add("WhatIsThis.BiddingShop");
                break;
            }
            case BUY: {
                if (CrazyAuctions.getInstance().isBuyingEnabled()) {
                    options.add("Shopping.Buying");
                }
                options.add("WhatIsThis.BuyingShop");
                break;
            }
            case ANY: {
                options.add("Shopping.Others");
                options.add("WhatIsThis.MainShop");
                break;
            }
        }
        // 添加选项按钮
        for (String o : options) {
            if (config.contains("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                if (!config.getBoolean("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                    continue;
                }
            }
            GUI.addStandardIcon(config, inventory, "Settings.GUISettings.OtherSettings." + o, o, oldLore -> {
                List<String> lore = new ArrayList<>();
                for (String l : oldLore) {
                    lore.add(l.replace("%category%", category.getDisplayName() != null
                            ? category.getDisplayName()
                            : category.getName()));
                }
                return lore;
            });
        }
        List<Integer> indexes = GUI.getInvIndexes(config, "Settings.GUISettings.OtherSettings.Content-Slots");
        icons.clear();
        icons.putAll(GUI.getPage(inventory, items, indexes, page));
    }

    @Override
    public void click(
            GlobalMarket market, InventoryAction action,
            int slot, ItemStack item, String itemFlag,
            InventoryClickEvent e
    ) {
        if (itemFlag != null) {
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
            if (itemFlag.equals("Refesh")) {
                PluginControl.updateCacheData();
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Shopping.Others")) {
                type = ShopType.SELL;
                page = 1;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Shopping.Selling")) {
                type = ShopType.BUY;
                page = 1;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Shopping.Buying")) {
                type = ShopType.BID;
                page = 1;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Shopping.Bidding")) {
                type = ShopType.ANY;
                page = 1;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("Items-Mail")) {
                run(() -> openPlayersMail(player, type, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Commoditys")) {
                run(() -> openPlayersCurrentList(player, type, category, 1));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Category")) {
                run(() -> openCategories(player, type, category));
                playClick(player);
                return;
            }
            if (itemFlag.equals("Custom")) {
                List<String> commands = config.getStringList("Settings.GUISettings.OtherSettings.Custom.Commands");
                for (String line : PAPI.setPlaceholders(player, commands)) {
                    String lower = line.toLowerCase();
                    if (lower.startsWith("server:")) {
                        String command = line.substring(7)
                                .replace("%player%", player.getName())
                                .replace("%player_uuid%", player.getUniqueId().toString());
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                    } else if (lower.startsWith("player:")) {
                        String command = line.substring(7)
                                .replace("%player%", player.getName())
                                .replace("%player_uuid%", player.getUniqueId().toString());
                        player.performCommand(command);
                    } else if (lower.startsWith("messages:")) {
                        String message = line.substring(9);
                        AdventureUtil.sendMessage(player, message);
                    } else if (lower.startsWith("message:")) {
                        String message = line.substring(8);
                        AdventureUtil.sendMessage(player, message);
                    }
                }
                playClick(player);
                if (config.getBoolean("Settings.GUISettings.OtherSettings.Custom.Close")) {
                    player.closeInventory();
                }
                return;
            }
            if (itemFlag.equals("Your-Item") || itemFlag.equals("Cant-Afford") || itemFlag.equals("Top-Bidder") || itemFlag.equals("Not-owned")) {
                return;
            }
        }
        MenuIcon icon = icons.get(slot);
        if (icon != null) {
            long uid = icon.uid;
            MarketGoods mg = market.getItems().stream().filter(goods -> goods.getUID() == uid).findFirst().orElse(null);
            if (mg != null) {
                if (PluginControl.hasMarketPermission(player, "Cancelled-Item")) {
                    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        UUID owner = mg.getItemOwner().getUUID();
                        Player p = Bukkit.getPlayer(owner);
                        if (p != null) {
                            MessageUtil.sendMessage(p, "Admin-Force-Cancelled-To-Player");
                        }
                        switch (mg.getShopType()) {
                            case BID: {
                                AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mg, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.BID);
                                Bukkit.getPluginManager().callEvent(event);
                                Storage playerData = Storage.getPlayer(Bukkit.getOfflinePlayer(owner));
                                if (mg.getTopBidder() != null && !mg.getTopBidder().equalsIgnoreCase("None")) {
                                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(mg.getTopBidder().split(":")[1]));
                                    if (op != null) {
                                        CurrencyManager.addMoney(op, mg.getPrice());
                                    }
                                }
                                playerData.addItem(new ItemMail(playerData.makeUID(), Bukkit.getOfflinePlayer(owner), mg.getItem(), mg.getFullTime(), System.currentTimeMillis(), false));
                                market.removeGoods(mg.getUID());
                                break;
                            }
                            case BUY: {
                                AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mg, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.BUY);
                                Bukkit.getPluginManager().callEvent(event);
                                CurrencyManager.addMoney(Bukkit.getOfflinePlayer(owner), mg.getReward());
                                market.removeGoods(uid);
                                break;
                            }
                            case SELL: {
                                AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mg, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.SELL);
                                Bukkit.getPluginManager().callEvent(event);
                                Storage playerData = Storage.getPlayer(Bukkit.getOfflinePlayer(owner));
                                playerData.addItem(new ItemMail(playerData.makeUID(), Bukkit.getOfflinePlayer(owner), mg.getItem(), mg.getFullTime(), System.currentTimeMillis(), false));
                                market.removeGoods(mg.getUID());
                                break;
                            }
                        }
                        MessageUtil.sendMessage(player, "Admin-Force-Cancelled");
                        playClick(player);
                        reopen();
                        return;
                    }
                }
                Runnable restoreItem = () -> inventory.setItem(slot, item);
                if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                    inventory.setItem(slot, makeStandardIcon(config, "Settings.GUISettings.OtherSettings.Your-Item", "Your-Item"));
                    playClick(player);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, restoreItem, 3 * 20);
                    return;
                }
                double cost = mg.getPrice();
                if (mg.getShopType().equals(ShopType.BUY)) {
                    cost = mg.getReward();
                }
                if (CurrencyManager.getMoney(player) < cost && !mg.getShopType().equals(ShopType.BUY)) {
                    inventory.setItem(slot, makeStandardIcon(config, "Settings.GUISettings.OtherSettings.Cant-Afford", "Cant-Afford"));
                    playClick(player);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, restoreItem, 3 * 20);
                    return;
                } else if (mg.getShopType().equals(ShopType.BUY) && PluginControl.hasNoMaterial(player, mg.getItem())) {
                    inventory.setItem(slot, makeStandardIcon(config, "Settings.GUISettings.OtherSettings.Not-owned", "Not-owned"));
                    playClick(player);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, restoreItem, 3 * 20);
                    return;
                }
                switch (mg.getShopType()) {
                    case BID: {
                        if (!mg.getTopBidder().equalsIgnoreCase("None") && UUID.fromString(mg.getTopBidder().split(":")[1]).equals(player.getUniqueId())) {
                            inventory.setItem(slot, makeStandardIcon(config, "Settings.GUISettings.Auction-Settings.Top-Bidder", "Top-Bidder"));
                            playClick(player);
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, restoreItem, 3 * 20);
                            return;
                        }
                        playClick(player);
                        run(() -> openBidding(player, mg));
                        break;
                    }
                    case BUY: {
                        playClick(player);
                        run(() -> openSelling(player, type, category, mg));
                        break;
                    }
                    case SELL: {
                        playClick(player);
                        run(() -> openBuying(player, type, category, mg));
                        break;
                    }
                }
                return;
            }
            playClick(player);
            page = 1;
            reopen();
            MessageUtil.sendMessage(player, "Item-Doesnt-Exist");
            return;
        }
        playClick(player);
    }
}
