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

public class GuiItemViewer extends AbstractGui {
    private final UUID uuid;
    private final Map<Integer, MenuIcon> icons = new HashMap<>();
    private int page;
    public GuiItemViewer(Player player, UUID uuid, int page) {
        super(player);
        this.uuid = uuid;
        this.page = page;
    }

    @Override
    protected void createInventory() {
        GlobalMarket market = GlobalMarket.getMarket();
        List<MenuIcon> items = new ArrayList<>();
        for (MarketGoods mg : market.getItems()) {
            if (mg.getItemOwner().getUUID().equals(uuid)) {
                List<String> lore = new ArrayList<>();
                if (mg.getShopType().equals(ShopType.BID) || mg.getShopType().equals(ShopType.ANY)) {
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
                if (mg.getShopType().equals(ShopType.BUY) || mg.getShopType().equals(ShopType.ANY)) {
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
                if (mg.getShopType().equals(ShopType.SELL) || mg.getShopType().equals(ShopType.ANY)) {
                    for (String l : MessageUtil.getValueList("SellingItemLore")) {
                        String price = String.valueOf(mg.getPrice());
                        String owner = mg.getItemOwner().getName();
                        String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
                        String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);
                        lore.add(l.replace("%price%", price)
                                .replace("%owner%", owner)
                                .replace("%addedtime%", addedTime)
                                .replace("%time%", time));
                    }
                    if (mg.getItem() == null) continue;
                    items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                }
            }
        }
        int maxPage = PluginControl.getMaxPage(items);
        while (page > maxPage) page--;
        inventory = create(54, PluginControl.color(player, config.getString("Settings.Player-Viewer-GUIName") + " #" + page));
        List<String> options = new ArrayList<>();
        options.add("WhatIsThis.Viewing");
        for (String o : options) {
            if (config.contains("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                if (!config.getBoolean("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                    continue;
                }
            }
            addStandardIcon(config, inventory, "Settings.GUISettings.OtherSettings." + o, o);
        }
        List<Integer> indexes = getInvIndexes(config, "Settings.GUISettings.OtherSettings.Content-Slots");
        icons.clear();
        icons.putAll(getPage(inventory, items, indexes, page));
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            if (itemFlag.equals("NextPage")) {
                PluginControl.updateCacheData();
                this.page++;
                reopen();
                playClick(player);
                return;
            }
            if (itemFlag.equals("PreviousPage")) {
                PluginControl.updateCacheData();
                this.page = Math.max(1, page - 1);
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
                        Bukkit.getScheduler().runTask(plugin, this::open);
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
                        run(() -> openSelling(player, ShopType.ANY, Category.getDefaultCategory(), mg));
                        break;
                    }
                    case SELL: {
                        playClick(player);
                        run(() -> openBuying(player, ShopType.ANY, Category.getDefaultCategory(), mg));
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
