package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionBuyEvent;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.util.*;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

import java.text.SimpleDateFormat;
import java.util.*;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.openShop;
import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.playClick;

public class GuiItemBuying extends AbstractGui {
    private final ShopType type;
    private final Category category;
    private final MarketGoods mg;
    public GuiItemBuying(Player player, ShopType type, Category category, MarketGoods mg) {
        super(player);
        this.type = type;
        this.category = category;
        this.mg = mg;
    }

    @Override
    protected void createInventory() {
        inventory = create(9, PluginControl.color(player, config.getString("Settings.Buying-Item")));
        List<String> options = new ArrayList<>();
        options.add("Confirm");
        options.add("Cancel");
        for (String o : options) {
            String id = config.getString("Settings.GUISettings.OtherSettings." + o + ".Item");
            String customModelKey = "Settings.GUISettings.OtherSettings." + o + ".CustomModelData";
            Integer customModel = config.contains(customModelKey) ? config.getInt(customModelKey) : null;
            String name = config.getString("Settings.GUISettings.OtherSettings." + o + ".Name");
            List<String> lore;
            if (config.contains("Settings.GUISettings.OtherSettings." + o + ".Lore")) {
                lore = config.getStringList("Settings.GUISettings.OtherSettings." + o + ".Lore");
            } else {
                lore = null;
            }
            ItemStack item = lore == null
                    ? PluginControl.makeItem(id, 1, name)
                    : PluginControl.makeItem(id, 1, name, lore);
            if (customModel != null) {
                AdventureItemStack.setCustomModelData(item, customModel);
            }
            if (o.equals("Confirm")) {
                inventory.setItem(0, item);
                inventory.setItem(1, item);
                inventory.setItem(2, item);
                inventory.setItem(3, item);
            }
            if (o.equals("Cancel")) {
                inventory.setItem(5, item);
                inventory.setItem(6, item);
                inventory.setItem(7, item);
                inventory.setItem(8, item);
            }
        }
        ItemStack item = mg.getItem();
        List<String> lore = new ArrayList<>();
        for (String l : MessageUtil.getValueList("SellingItemLore")) {
            lore.add(l.replace("%price%", String.valueOf(mg.getPrice())).replace("%addedtime%", new SimpleDateFormat(MessageUtil.getValue("Date-Format")).format(new Date(mg.getAddedTime()))).replace("%owner%", mg.getItemOwner().getName()).replace("%time%", PluginControl.convertToTime(mg.getTimeTillExpire(), false)));
        }
        inventory.setItem(4, PluginControl.addLore(item.clone(), lore));
    }

    @Override
    protected void click(GlobalMarket market, InventoryAction action, int slot, ItemStack item, String itemFlag, InventoryClickEvent e) {
        if (itemFlag != null) {
            if (itemFlag.equals("Confirm")) {
                if (mg == null) {
                    playClick(player);
                    run(() -> openShop(player, type, category, 1));
                    MessageUtil.sendMessage(player, "Item-Doesnt-Exist");
                    return;
                }
                if (PluginControl.isInvFull(player)) {
                    playClick(player);
                    player.closeInventory();
                    MessageUtil.sendMessage(player, "Inventory-Full");
                    return;
                }
                if (CurrencyManager.getMoney(player) < mg.getPrice()) {
                    playClick(player);
                    player.closeInventory();
                    HashMap<String, String> placeholders = new HashMap<>();
                    placeholders.put("%Money_Needed%", String.valueOf(mg.getPrice() - CurrencyManager.getMoney(player)));
                    placeholders.put("%money_needed%", String.valueOf(mg.getPrice() - CurrencyManager.getMoney(player)));
                    MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                    return;
                }
                UUID owner = mg.getItemOwner().getUUID();
                Bukkit.getPluginManager().callEvent(new AuctionBuyEvent(player, mg, mg.getPrice()));
                CurrencyManager.removeMoney(player, mg.getPrice());
                CurrencyManager.addMoney(PluginControl.getOfflinePlayer(owner), mg.getPrice());
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%Price%", String.valueOf(mg.getPrice()));
                placeholders.put("%price%", String.valueOf(mg.getPrice()));
                placeholders.put("%Player%", player.getName());
                placeholders.put("%player%", player.getName());
                MessageUtil.sendMessage(player, "Bought-Item", placeholders);
                if (PluginControl.isOnline(owner) && PluginControl.getPlayer(owner) != null) {
                    Player p = PluginControl.getPlayer(owner);
                    MessageUtil.sendMessage(p, "Player-Bought-Item", placeholders);
                }
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(mg.getItem());
                } else {
                    player.getWorld().dropItem(player.getLocation(), mg.getItem());
                }
                market.removeGoods(mg.getUID());
                playClick(player);
                run(() -> openShop(player, type, category, 1));
                return;
            }
            if (itemFlag.equals("Cancel")) {
                run(() -> openShop(player, type, category, 1));
                playClick(player);
                return;
            }
        }
    }
}
