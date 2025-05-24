package top.mrxiaom.crazyauctions.reloaded.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.scheduler.BukkitTask;
import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionWinBidEvent;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.data.ItemMail;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;

public class AuctionProcess {
    private static BukkitTask task;

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public static void start(double updateDelay) {
        stop();
        long interval = (long)(updateDelay * 1000L) / 50L;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            try {
                updateAuctionData();
            } catch (Exception ex) {
                PluginControl.printStackTrace(ex);
            }
        }, interval, interval);
    }

    public static void updateAuctionData() {
        if (FileManager.isBackingUp()) return;
        if (FileManager.isRollingBack()) return;
        if (FileManager.isSyncing()) return;
        FileManager.ProtectedConfiguration config = FileManager.Files.CONFIG.getFile();
        GlobalMarket market = GlobalMarket.getMarket();
        List<MarketGoods> array = market.getItems();
        if (!array.isEmpty()) {
            for (int i = array.size() - 1;i > -1;i--) {
                MarketGoods mg = array.get(i);
                if (mg == null) {
                    continue;
                }
                if (mg.getItem() == null) {
                    market.removeGoods(mg);
                    continue;
                }
                if (mg.getShopType().equals(ShopType.BID)) {
                    if (mg.expired()) {
                        if (mg.getTopBidder().equalsIgnoreCase("None")) {
                            Storage playerData = Storage.getPlayer(mg.getItemOwner().getUUID());
                            Map<String, String> placeholders = new HashMap<>();
                            String item;
                            ItemMeta meta = mg.getItem().getItemMeta();
                            if (meta == null) {
                                item = mg.getItem().getType().toString().toLowerCase().replace("_", " ");
                            } else {
                                try {
                                    item = meta.hasDisplayName() ? meta.getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem());
                                } catch (ReflectiveOperationException ex) {
                                    item = meta.hasDisplayName() ? meta.getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " ");
                                }
                            }
                            placeholders.put("%item%", item);
                            ItemMail im = new ItemMail(playerData.makeUID(), mg.getItemOwner().getUUID(), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), mg.getAddedTime(), false);
                            playerData.addItem(im);
                            market.removeGoods(mg.getUID());
                            if (mg.getItemOwner().getPlayer() != null) {
                                MessageUtil.sendMessage(mg.getItemOwner().getPlayer(), "Item-Has-Expired", placeholders);
                            }
                        } else {
                            UUID buyer = UUID.fromString(mg.getTopBidder().split(":")[1]);
                            UUID seller = mg.getItemOwner().getUUID();
                            CurrencyManager.addMoney(Bukkit.getOfflinePlayer(seller), mg.getPrice());
                            Storage playerData = Storage.getPlayer(buyer);
                            ItemMail im = new ItemMail(playerData.makeUID(), PluginControl.getOfflinePlayer(buyer), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), mg.getAddedTime(), false);
                            playerData.addItem(im);
                            market.removeGoods(mg.getUID());
                            double price = mg.getPrice();
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%price%", String.valueOf(mg.getPrice()));
                            placeholders.put("%player%", PluginControl.getOfflinePlayer(buyer).getName());
                            if (PluginControl.isOnline(buyer) && PluginControl.getPlayer(buyer) != null) {
                                Player player = Bukkit.getPlayer(buyer);
                                AuctionWinBidEvent event = new AuctionWinBidEvent(player, mg, price);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Bukkit.getPluginManager().callEvent(event);
                                    }
                                }.runTask(Main.getInstance());
                                MessageUtil.sendMessage(player, "Win-Bidding", placeholders);
                            }
                            Player player = Bukkit.getPlayer(seller);
                            if (player != null) {
                                MessageUtil.sendMessage(player, "Someone-Won-Players-Bid", placeholders);
                            }
                        }
                    } else {
                        if (config.getBoolean("Settings.Auction-Process-Settings.Countdown-Tips.Enabled")) {
                            long l = (mg.getTimeTillExpire() - System.currentTimeMillis()) / 1000;
                            if (config.get("Settings.Auction-Process-Settings.Countdown-Tips.Times." + l) != null) {
                                String item = LangUtilsHook.getItemName(mg.getItem());
                                for (String message : config.getStringList("Settings.Auction-Process-Settings.Countdown-Tips.Times." + l)) {
                                    Bukkit.broadcastMessage(message.replace("%owner%", mg.getItemOwner().getName()).replace("%item%", item).replace("{prefix}", PluginControl.getPrefix()).replace("&", "§"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
