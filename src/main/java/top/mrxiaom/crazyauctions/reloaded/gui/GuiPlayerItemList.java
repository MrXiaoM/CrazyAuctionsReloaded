package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionCancelledEvent;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;
import top.mrxiaom.crazyauctions.reloaded.util.*;
import top.mrxiaom.crazyauctions.reloaded.util.enums.CancelledReason;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

import java.text.SimpleDateFormat;
import java.util.*;

import static top.mrxiaom.crazyauctions.reloaded.event.GuiManager.repricing;
import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.*;

public class GuiPlayerItemList extends AbstractGui {
    private final ShopType type;
    private final Category category;
    private int page;
    private final Map<Integer, MenuIcon> icons = new HashMap<>();
    public GuiPlayerItemList(Player player, ShopType type, Category category, int page) {
        super(player);
        this.type = type;
        this.category = category;
        this.page = page;
    }

    @Override
    protected void createInventory() {
        List<MenuIcon> items = new ArrayList<>();
        GlobalMarket market = GlobalMarket.getMarket();
        inventory = create(54, PluginControl.color(player, config.getString("Settings.Player-Items-List")));
        List<String> options = new ArrayList<>();
        options.add("Player-Items-List-Back");
        options.add("WhatIsThis.CurrentItems");
        for (String o : options) {
            if (config.contains("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                if (!config.getBoolean("Settings.GUISettings.OtherSettings." + o + ".Toggle")) {
                    continue;
                }
            }
            addStandardIcon(config, inventory, "Settings.GUISettings.OtherSettings." + o, o);
        }
        for (MarketGoods mg : market.getItems()) {
            if (mg.getItemOwner().getUUID().equals(player.getUniqueId())) {
                List<String> lore = new ArrayList<>();
                if (mg.getShopType().equals(ShopType.BID) || mg.getShopType().equals(ShopType.ANY)) {
                    String owner = mg.getItemOwner().getName();
                    String topBidder = mg.getTopBidder().split(":")[0];
                    for (String l : MessageUtil.getValueList("CurrentBiddingItemLore")) {
                        lore.add(l.replace("%price%", String.valueOf(mg.getPrice())).replace("%topbid%", String.valueOf(mg.getPrice())).replace("%owner%", owner).replace("%topbidder%", topBidder).replace("%addedtime%", new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()))).replace("%time%", PluginControl.convertToTime(mg.getTimeTillExpire(), false)));
                    }
                    if (mg.getItem() == null) continue;
                    items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                }
                if (mg.getShopType().equals(ShopType.BUY) || mg.getShopType().equals(ShopType.ANY)) {
                    for (String l : MessageUtil.getValueList("CurrentBuyingItemLore")) {
                        String reward = String.valueOf(mg.getReward());
                        String owner = mg.getItemOwner().getName();
                        lore.add(l.replace("%reward%", reward)
                                .replace("%Owner%", owner) .replace("%owner%", owner)
                                .replace("%addedtime%", new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime())))
                                .replace("%time%", PluginControl.convertToTime(mg.getTimeTillExpire(), false)));
                    }
                    items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                }
                if (mg.getShopType().equals(ShopType.SELL) || mg.getShopType().equals(ShopType.ANY)) {
                    for (String l : MessageUtil.getValueList("CurrentSellingItemLore")) {
                        lore.add(l.replace("%price%", String.valueOf(mg.getPrice())).replace("%Owner%", mg.getItemOwner().getName()).replace("%owner%", mg.getItemOwner().getName()).replace("%addedtime%", new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()))).replace("%time%", PluginControl.convertToTime(mg.getTimeTillExpire(), false)));
                    }
                    if (mg.getItem() == null) continue;
                    items.add(MenuIcon.icon(mg.getUID(), PluginControl.addLore(mg.getItem().clone(), lore)));
                }
            }
        }
        List<Integer> indexes = getInvIndexes(config, "Settings.GUISettings.OtherSettings.Content-Slots");
        icons.clear();
        icons.putAll(getPage(inventory, items, indexes, page));
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            if (itemFlag.equals("Player-Items-List-Back")) {
                run(() -> openShop(player, type, category, 1));
                playClick(player);
                return;
            }

            MenuIcon icon = icons.get(slot);
            if (icon != null) {
                long uid = icon.uid;
                boolean Repricing = e.getClick().equals(ClickType.RIGHT) || e.getClick().equals(ClickType.SHIFT_RIGHT);
                MarketGoods mg = market.getMarketGoods(uid);
                if (mg == null) {
                    playClick(player);
                    run(() -> openShop(player, type, category, 1));
                    MessageUtil.sendMessage(player, "Item-Doesnt-Exist");
                } else switch (mg.getShopType()) {
                    case BID: {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(player, "Cancelled-Item-On-Bid", placeholders);
                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.BID);
                        Bukkit.getPluginManager().callEvent(event);
                        if (mg.getTopBidder() != null && !mg.getTopBidder().equalsIgnoreCase("None")) {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(mg.getTopBidder().split(":")[1]));
                            if (op != null) {
                                CurrencyManager.addMoney(op, mg.getPrice());
                            }
                        }
                        Storage playerData = Storage.getPlayer(mg.getItemOwner().getUUID());
                        playerData.addItem(new ItemMail(playerData.makeUID(), mg.getItemOwner().getUUID(), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), System.currentTimeMillis(), false));
                        market.removeGoods(uid);
                        repricing.remove(player.getUniqueId());
                        playClick(player);
                        page = 1;
                        reopen();
                        return;
                    }
                    case BUY: {
                        if (Repricing) {
                            repricing.put(player.getUniqueId(), new Object[]{mg, String.valueOf(System.currentTimeMillis() + (config.getInt("Settings.Repricing-Timeout") * 1000L))});
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                            placeholders.put("%timeout%", config.getString("Settings.Repricing-Timeout"));
                            MessageUtil.sendMessage(player, "Repricing", placeholders);
                            playClick(player);
                            player.closeInventory();
                            return;
                        }
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%reward%", String.valueOf(mg.getReward()));
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(player, "Cancelled-Item-On-Buy", placeholders);
                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.BUY);
                        Bukkit.getPluginManager().callEvent(event);
                        CurrencyManager.addMoney(player, mg.getReward());
                        market.removeGoods(uid);
                        repricing.remove(player.getUniqueId());
                        playClick(player);
                        page = 1;
                        reopen();
                        return;
                    }
                    case SELL: {
                        if (Repricing) {
                            repricing.put(player.getUniqueId(), new Object[]{mg, String.valueOf(System.currentTimeMillis() + (config.getInt("Settings.Repricing-Timeout") * 1000L))});
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                            placeholders.put("%timeout%", config.getString("Settings.Repricing-Timeout"));
                            MessageUtil.sendMessage(player, "Repricing", placeholders);
                            playClick(player);
                            player.closeInventory();
                            return;
                        }
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(player, "Cancelled-Item-On-Sale", placeholders);
                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.SELL);
                        Bukkit.getPluginManager().callEvent(event);
                        Storage playerData = Storage.getPlayer(mg.getItemOwner().getUUID());
                        playerData.addItem(new ItemMail(playerData.makeUID(), mg.getItemOwner().getUUID(), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), System.currentTimeMillis(), false));
                        market.removeGoods(uid);
                        repricing.remove(player.getUniqueId());
                        playClick(player);
                        page = 1;
                        reopen();
                        return;
                    }
                }
                return;
            }
        }
    }
}
