package top.mrxiaom.crazyauctions.reloaded.event;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.gui.IGui;
import top.mrxiaom.crazyauctions.reloaded.util.*;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * MrXiaoM/PluginBase
 */
public class GuiManager implements Listener {
    public final static Map<UUID, Object[]> repricing = new HashMap<>();
    private static GuiManager instance;
    private final Main plugin;
    private final Map<UUID, IGui> playersGui = new HashMap<>();
    BiConsumer<Player, IGui> disable = (player, gui) -> {
        try {
            AdventureUtil.sendTitle(player, "&e请等等", "&f管理员正在更新插件", 10, 30, 10);
        } catch (Throwable ignored) {}
    };
    boolean disabled = false;
    public GuiManager(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        instance = this;
    }

    public void openGui(IGui gui) {
        if (disabled) return;
        Player player = gui.getPlayer();
        if (player == null) return;
        player.closeInventory();
        playersGui.put(player.getUniqueId(), gui);
        Inventory inv = gui.newInventory();
        if (inv != null) {
            player.openInventory(inv);
        } else if (!gui.allowNullInventory()) {
            plugin.getLogger().warning("试图为玩家 " + player.getName() + " 打开界面 " + gui.getClass().getName() + " 时，程序返回了 null");
        }
    }

    public void onDisable() {
        disabled = true;
        List<Map.Entry<UUID, IGui>> entries = Lists.newArrayList(playersGui.entrySet());
        for (Map.Entry<UUID, IGui> entry : entries) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            entry.getValue().onClose(player.getOpenInventory());
            playersGui.remove(entry.getKey());
            player.closeInventory();
            if (disable != null) disable.accept(player, entry.getValue());
        }
    }

    public void setDisableAction(@Nullable BiConsumer<Player, IGui> consumer) {
        this.disable = consumer;
    }

    @Nullable
    public IGui getOpeningGui(Player player) {
        if (disabled) return null;
        return playersGui.get(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (disabled) return;
        Player player = e.getPlayer();
        IGui remove = playersGui.remove(player.getUniqueId());
        if (remove != null) {
            remove.onClose(player.getOpenInventory());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (disabled || !(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (playersGui.containsKey(player.getUniqueId())) {
            playersGui.get(player.getUniqueId()).onClick(event.getAction(), event.getClick(), event.getSlotType(),
                    event.getRawSlot(), event.getCurrentItem(), event.getCursor(), event.getView(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (disabled || !(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (playersGui.containsKey(player.getUniqueId())) {
            playersGui.get(player.getUniqueId()).onDrag(event.getView(), event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (disabled || !(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        IGui remove = playersGui.remove(player.getUniqueId());
        if (remove != null) {
            remove.onClose(event.getView());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRepricing(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (repricing.get(player.getUniqueId()) != null) {
            FileManager.ProtectedConfiguration config = FileManager.Files.CONFIG.getFile();
            if (!PluginControl.isNumber(e.getMessage())) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%Arg%", e.getMessage());
                placeholders.put("%arg%", e.getMessage());
                MessageUtil.sendMessage(player, "Not-A-Valid-Number", placeholders);
                repricing.remove(player.getUniqueId());
                e.setCancelled(true);
                return;
            }
            MarketGoods mg;
            try {
                mg = (MarketGoods) repricing.get(player.getUniqueId())[0];
            } catch (ClassCastException ex) {
                PluginControl.printStackTrace(ex);
                return;
            }
            if (mg != null && mg.getItem() != null) {
                double money = Double.parseDouble(e.getMessage());
                switch (mg.getShopType()) {
                    case BUY: {
                        if (money < config.getDouble("Settings.Minimum-Buy-Reward")) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%reward%", String.valueOf(config.getDouble("Settings.Minimum-Buy-Reward")));
                            MessageUtil.sendMessage(player, "Buy-Reward-To-Low", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (money > config.getLong("Settings.Max-Beginning-Buy-Reward")) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%reward%", String.valueOf(config.getDouble("Settings.Max-Beginning-Buy-Reward")));
                            MessageUtil.sendMessage(player, "Buy-Reward-To-High", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (CurrencyManager.getMoney(player) < money) {
                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Money_Needed%", String.valueOf(money - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf(money - CurrencyManager.getMoney(player)));
                            MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        double tax = 0;
                        if (PluginControl.notBypassTaxRate(player, ShopType.BUY)) {
                            tax = money * PluginControl.getTaxRate(player, ShopType.BUY);
                        }
                        if (CurrencyManager.getMoney(player) < money + tax) {
                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Money_Needed%", String.valueOf((money + tax) - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf((money + tax) - CurrencyManager.getMoney(player)));
                            MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                            e.setCancelled(true);
                            return;
                        }
                        CurrencyManager.removeMoney(player, money + tax);
                        CurrencyManager.addMoney(player, mg.getReward());
                        mg.setPrice(money);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%money%", String.valueOf(money));
                        placeholders.put("%tax%", String.valueOf(tax));
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(player, "Repricing-Succeeded", placeholders);
                        repricing.remove(player.getUniqueId());
                        e.setCancelled(true);
                        break;
                    }
                    case SELL: {
                        if (money < config.getDouble("Settings.Minimum-Sell-Price")) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%price%", String.valueOf(config.getDouble("Settings.Minimum-Sell-Price")));
                            MessageUtil.sendMessage(player, "Sell-Price-To-Low", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (money > config.getLong("Settings.Max-Beginning-Sell-Price")) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%price%", String.valueOf(config.getDouble("Settings.Max-Beginning-Sell-Price")));
                            MessageUtil.sendMessage(player, "Sell-Price-To-High", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        double tax = 0;
                        if (PluginControl.notBypassTaxRate(player, ShopType.SELL)) {
                            tax = money * PluginControl.getTaxRate(player, ShopType.SELL);
                        }
                        if (CurrencyManager.getMoney(player) < tax) {
                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Money_Needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                            MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                            e.setCancelled(true);
                            return;
                        }
                        CurrencyManager.removeMoney(player, tax);
                        mg.setPrice(money);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%money%", String.valueOf(money));
                        placeholders.put("%tax%", String.valueOf(tax));
                        placeholders.put("%item%", LangUtilsHook.getItemName(mg.getItem()));
                        MessageUtil.sendMessage(player, "Repricing-Succeeded", placeholders);
                        repricing.remove(player.getUniqueId());
                        e.setCancelled(true);
                        break;
                    }
                }
            } else {
                MessageUtil.sendMessage(player, "Repricing-Failed");
                repricing.remove(player.getUniqueId());
                e.setCancelled(true);
            }
        }
    }

    public static GuiManager inst() {
        return instance;
    }
}
