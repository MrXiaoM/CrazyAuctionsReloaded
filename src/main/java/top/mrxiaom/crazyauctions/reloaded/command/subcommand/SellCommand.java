package top.mrxiaom.crazyauctions.reloaded.command.subcommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import top.mrxiaom.crazyauctions.reloaded.api.events.AuctionListEvent;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.currency.CurrencyManager;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager;
import top.mrxiaom.crazyauctions.reloaded.data.ItemOwner;
import top.mrxiaom.crazyauctions.reloaded.data.MarketGoods;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.data.ShopType;
import top.mrxiaom.crazyauctions.reloaded.util.enums.Version;

public class SellCommand
    implements CrazyAuctionsSubCommand
{
    @Override
    public void execute(CommandSender sender, String subCommand, String... args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "Players-Only");
            return;
        }
        if (args.length >= 2) {
            Player player = (Player) sender;
            if (PluginControl.isWorldDisabled(player)) {
                MessageUtil.sendMessage(sender, "World-Disabled");
                return;
            }
            ShopType type = ShopType.SELL;
            if (!CrazyAuctionsCommand.getCrazyAuctions().isSellingEnabled()) {
                MessageUtil.sendMessage(player, "Selling-Disabled");
                return;
            }
            if (!PluginControl.hasCommandPermission(player, "Sell", true)) {
                MessageUtil.sendMessage(player, "No-Permission");
                return;
            }
            ItemStack item = PluginControl.getItemInHand(player);
            int amount = item.getAmount();
            if (args.length >= 3) {
                if (PluginControl.isNotInt(args[2])) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%arg%", args[2]);
                    MessageUtil.sendMessage(player, "Not-A-Valid-Number", placeholders);
                    return;
                }
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) amount = 1;
                if (amount > item.getAmount()) amount = item.getAmount();
            }
            if (PluginControl.getItemInHand(player).getType() == Material.AIR) {
                MessageUtil.sendMessage(player, "Doesnt-Have-Item-In-Hand");
                return;
            }
            if (!PluginControl.isNumber(args[1])) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%arg%", args[1]);
                MessageUtil.sendMessage(player, "Not-A-Valid-Number", placeholders);
                return;
            }
            double price = Double.parseDouble(args[1]);
            double tax = 0;
            if (!CrazyAuctionsCommand.getCrazyAuctions().isSellingEnabled()) {
                MessageUtil.sendMessage(player, "Selling-Disable");
                return;
            }
            if (!PluginControl.hasCommandPermission(player, "Sell", true)) {
                MessageUtil.sendMessage(player, "No-Permission");
                return;
            }
            if (price < FileManager.Files.CONFIG.getFile().getDouble("Settings.Minimum-Sell-Price")) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%price%", String.valueOf(FileManager.Files.CONFIG.getFile().getDouble("Settings.Minimum-Sell-Price")));
                MessageUtil.sendMessage(player, "Sell-Price-To-Low", placeholders);
                return;
            }
            if (price > FileManager.Files.CONFIG.getFile().getDouble("Settings.Max-Beginning-Sell-Price")) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%price%", String.valueOf(FileManager.Files.CONFIG.getFile().getDouble("Settings.Max-Beginning-Sell-Price")));
                MessageUtil.sendMessage(player, "Sell-Price-To-High", placeholders);
                return;
            }
            if (PluginControl.notBypassLimit(player, ShopType.SELL)) {
                int limit = PluginControl.getLimit(player, ShopType.SELL);
                if (limit > -1) {
                    if (CrazyAuctionsCommand.getCrazyAuctions().getNumberOfPlayerItems(player, ShopType.SELL) >= limit) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%number%", String.valueOf(limit));
                        MessageUtil.sendMessage(player, "Max-Selling-Items", placeholders);
                        return;
                    }
                }
            }
            if (PluginControl.notBypassTaxRate(player, ShopType.SELL)) {
                tax = price * PluginControl.getTaxRate(player, ShopType.SELL);
                if (CurrencyManager.getMoney(player) < tax) { 
                    HashMap<String, String> placeholders = new HashMap<>();
                    placeholders.put("%Money_Needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                    placeholders.put("%money_needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                    MessageUtil.sendMessage(player, "Need-More-Money", placeholders);
                    return;
                }
            }
            if (PluginControl.isItemBlacklisted(item)) {
                MessageUtil.sendMessage(player, "Item-BlackListed");
                return;
            }
            if (PluginControl.isItemLoreBlacklisted(item)) {
                MessageUtil.sendMessage(player, "Item-LoreBlackListed");
                return;
            }
            if (!FileManager.Files.CONFIG.getFile().getBoolean("Settings.Allow-Damaged-Items")) {
                for (Material i : getDamageableItems()) {
                    if (item.getType() == i) {
                        if (item.getDurability() > 0) {
                            MessageUtil.sendMessage(player, "Item-Damaged");
                            return;
                        }
                    }
                }
            }
            UUID owner = player.getUniqueId();
            ItemStack is = item.clone();
            is.setAmount(amount);
            GlobalMarket market = GlobalMarket.getMarket();
            MarketGoods goods = new MarketGoods(
                market.makeUID(),
                type,
                new ItemOwner(owner, player.getName()),
                is,
                PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Sell-Time")),
                PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")),
                System.currentTimeMillis(),
                price,
                "None"
            );
            market.addGoods(goods);
            Bukkit.getPluginManager().callEvent(new AuctionListEvent(player, type, is, price, tax));
            CurrencyManager.removeMoney(player, tax);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%Price%", String.valueOf(price));
            placeholders.put("%price%", String.valueOf(price));
            placeholders.put("%tax%", String.valueOf(tax));
            MessageUtil.sendMessage(player, "Added-Item-For-Sale", placeholders);
            if (item.getAmount() <= 1 || (item.getAmount() - amount) <= 0) {
                PluginControl.setItemInHand(player, new ItemStack(Material.AIR));
            } else {
                item.setAmount(item.getAmount() - amount);
            }
            return;
        }
        MessageUtil.sendMessage(sender, "CrazyAuctions-Sell");
    }

    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String subCommand, String... args) {
        return new ArrayList<>();
    }

    @Override
    public CrazyAuctionsSubCommandType getCommandType() {
        return CrazyAuctionsSubCommandType.SELL;
    }
    
    private ArrayList<Material> getDamageableItems() {
        ArrayList<Material> ma = new ArrayList<>();
        if (Version.getCurrentVersion().isNewer(Version.v1_12_R1)) {
            ma.add(Material.matchMaterial("GOLDEN_HELMET"));
            ma.add(Material.matchMaterial("GOLDEN_CHESTPLATE"));
            ma.add(Material.matchMaterial("GOLDEN_LEGGINGS"));
            ma.add(Material.matchMaterial("GOLDEN_BOOTS"));
            ma.add(Material.matchMaterial("WOODEN_SWORD"));
            ma.add(Material.matchMaterial("WOODEN_AXE"));
            ma.add(Material.matchMaterial("WOODEN_PICKAXE"));
            ma.add(Material.matchMaterial("WOODEN_AXE"));
            ma.add(Material.matchMaterial("WOODEN_SHOVEL"));
            ma.add(Material.matchMaterial("STONE_SHOVEL"));
            ma.add(Material.matchMaterial("IRON_SHOVEL"));
            ma.add(Material.matchMaterial("DIAMOND_SHOVEL"));
            ma.add(Material.matchMaterial("WOODEN_HOE"));
            ma.add(Material.matchMaterial("GOLDEN_HOE"));
            ma.add(Material.matchMaterial("CROSSBOW"));
            ma.add(Material.matchMaterial("TRIDENT"));
            ma.add(Material.matchMaterial("TURTLE_HELMET"));
        } else {
            ma.add(Material.matchMaterial("GOLD_HELMET"));
            ma.add(Material.matchMaterial("GOLD_CHESTPLATE"));
            ma.add(Material.matchMaterial("GOLD_LEGGINGS"));
            ma.add(Material.matchMaterial("GOLD_BOOTS"));
            ma.add(Material.matchMaterial("WOOD_SWORD"));
            ma.add(Material.matchMaterial("WOOD_AXE"));
            ma.add(Material.matchMaterial("WOOD_PICKAXE"));
            ma.add(Material.matchMaterial("WOOD_AXE"));
            ma.add(Material.matchMaterial("WOOD_SPADE"));
            ma.add(Material.matchMaterial("STONE_SPADE"));
            ma.add(Material.matchMaterial("IRON_SPADE"));
            ma.add(Material.matchMaterial("DIAMOND_SPADE"));
            ma.add(Material.matchMaterial("WOOD_HOE"));
            ma.add(Material.matchMaterial("GOLD_HOE"));
        }
        ma.add(Material.DIAMOND_HELMET);
        ma.add(Material.DIAMOND_CHESTPLATE);
        ma.add(Material.DIAMOND_LEGGINGS);
        ma.add(Material.DIAMOND_BOOTS);
        ma.add(Material.CHAINMAIL_HELMET);
        ma.add(Material.CHAINMAIL_CHESTPLATE);
        ma.add(Material.CHAINMAIL_LEGGINGS);
        ma.add(Material.CHAINMAIL_BOOTS);
        ma.add(Material.IRON_HELMET);
        ma.add(Material.IRON_CHESTPLATE);
        ma.add(Material.IRON_LEGGINGS);
        ma.add(Material.IRON_BOOTS);
        ma.add(Material.LEATHER_HELMET);
        ma.add(Material.LEATHER_CHESTPLATE);
        ma.add(Material.LEATHER_LEGGINGS);
        ma.add(Material.LEATHER_BOOTS);
        ma.add(Material.BOW);
        ma.add(Material.STONE_SWORD);
        ma.add(Material.IRON_SWORD);
        ma.add(Material.DIAMOND_SWORD);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.STONE_PICKAXE);
        ma.add(Material.IRON_PICKAXE);
        ma.add(Material.DIAMOND_PICKAXE);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.STONE_HOE);
        ma.add(Material.IRON_HOE);
        ma.add(Material.DIAMOND_HOE);
        ma.add(Material.FLINT_AND_STEEL);
        ma.add(Material.ANVIL);
        ma.add(Material.FISHING_ROD);
        return ma;
    }
}
