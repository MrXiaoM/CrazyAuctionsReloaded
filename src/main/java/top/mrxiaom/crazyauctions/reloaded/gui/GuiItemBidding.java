package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionNewBidEvent;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

import java.text.SimpleDateFormat;
import java.util.*;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.makeStandardIcon;
import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.playClick;

public class GuiItemBidding extends AbstractGui {
    private final MarketGoods mg;
    private int bid;
    public GuiItemBidding(Player player, MarketGoods mg) {
        super(player);
        this.mg = mg;
        this.bid = (int) mg.getPrice();
    }

    @Override
    protected void createInventory() {
        inventory = create(27, PluginControl.color(player, config.getString("Settings.Bidding-On-Item")));
        ConfigurationSection section = config.getConfig().getConfigurationSection("Settings.GUISettings.Auction-Settings.Bidding-Buttons");
        if (section != null) for (String price : section.getKeys(false)) {
            List<Integer> slots = config.getConfig().getIntegerList("Settings.GUISettings.Auction-Settings.Bidding-Buttons." + price + ".Slots");
            if (config.contains("Settings.GUISettings.Auction-Settings.Bidding-Buttons." + price + ".Slot")) {
                slots.add(config.getInt("Settings.GUISettings.Auction-Settings.Bidding-Buttons." + price + ".Slot"));
            }
            ItemStack item = makeStandardIcon(config, "Settings.GUISettings.Auction-Settings.Bidding-Buttons." + price, "Bidding-Buttons." + price);
            for (int slot : slots) {
                inventory.setItem(slot, item);
            }
        }
        inventory.setItem(13, getBiddingGlass());
        inventory.setItem(22, makeStandardIcon(config, "Settings.GUISettings.Auction-Settings.Bid", "Bidding-Buttons.Bid"));
        inventory.setItem(4, getBiddingItem());
    }

    public ItemStack getBiddingGlass() {
        // 这个图标不需要处理点击操作
        String price = String.valueOf(mg.getPrice());

        return makeStandardIcon(config, "Settings.GUISettings.Auction-Settings.Bidding", null, oldLore -> {
            List<String> lore = new ArrayList<>();
            for (String l : oldLore) {
                lore.add(l.replace("%bid%", String.valueOf(bid))
                        .replace("%topbid%", price));
            }
            return lore;
        });
    }

    public ItemStack getBiddingItem() {
        // 这个图标不需要处理点击操作
        String price = String.valueOf(mg.getPrice());
        String owner = mg.getItemOwner().getName();
        String topBidder = mg.getTopBidder().split(":")[0];
        String addedTime = new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()));
        String time = PluginControl.convertToTime(mg.getTimeTillExpire(), false);

        ItemStack item = mg.getItem();
        List<String> lore = new ArrayList<>();
        for (String l : MessageUtil.getValueList("BiddingItemLore")) {
            lore.add(l.replace("%topbid%", price)
                    .replace("%owner%", owner)
                    .replace("%topbidder%", topBidder)
                    .replace("%addedtime%", addedTime)
                    .replace("%time%", time));
        }
        return PluginControl.addLore(item.clone(), lore);
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            if (itemFlag.equals("Bidding-Buttons.Bid")) {
                if (mg == null) {
                    e.setCancelled(true);
                    player.closeInventory();
                    return;
                }
                String topBidder = mg.getTopBidder();
                if (CurrencyManager.getMoney(player) < bid) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%Money_Needed%", String.valueOf(bid - CurrencyManager.getMoney(player)));
                    placeholders.put("%money_needed%", String.valueOf(bid - CurrencyManager.getMoney(player)));
                    MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                    return;
                }
                if (mg.getPrice() > bid) {
                    MessageUtil.sendMessage(player, "Bid-More-Money");
                    return;
                }
                if (mg.getPrice() >= bid && !topBidder.equalsIgnoreCase("None")) {
                    MessageUtil.sendMessage(player, "Bid-More-Money");
                    return;
                }
                if (!topBidder.equalsIgnoreCase("None")) {
                    String[] oldTopBidder = mg.getTopBidder().split(":");
                    CurrencyManager.addMoney(Bukkit.getOfflinePlayer(UUID.fromString(oldTopBidder[1])), mg.getPrice());
                }
                Bukkit.getPluginManager().callEvent(new AuctionNewBidEvent(player, mg, bid));
                CurrencyManager.removeMoney(player, bid);
                mg.setPrice(bid);
                mg.setTopBidder(player.getName() + ":" + player.getUniqueId());
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%Bid%", String.valueOf(bid));
                placeholders.put("%bid%", String.valueOf(bid));
                MessageUtil.sendMessage(player, "Bid-Msg", placeholders);
                bid = 0;
                player.closeInventory();
                playClick(player);
                return;
            } else {
                Map<String, Integer> priceEdits = new HashMap<>();
                ConfigurationSection section = config.getConfig().getConfigurationSection("Settings.GUISettings.Auction-Settings.Bidding-Buttons");
                if (section != null) for (String price : section.getKeys(false)) {
                    if (PluginControl.isNumber(price)) {
                        String name = "Bidding-Buttons." + price;
                        priceEdits.put(name, Integer.valueOf(price));
                    }
                }
                for (String price : priceEdits.keySet()) {
                    if (itemFlag.equals(price)) {
                        try {
                            bid = bid + priceEdits.get(price);
                            inventory.setItem(4, getBiddingItem());
                            inventory.setItem(13, getBiddingGlass());
                            playClick(player);
                            return;
                        } catch (Exception ex) {
                            player.closeInventory();
                            MessageUtil.sendMessage(player, "Item-Doesnt-Exist");
                            return;
                        }
                    }
                }
            }
        }
    }
}
