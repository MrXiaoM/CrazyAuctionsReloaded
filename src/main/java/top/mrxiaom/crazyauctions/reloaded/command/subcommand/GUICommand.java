package top.mrxiaom.crazyauctions.reloaded.command.subcommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.gui.GUI;
import top.mrxiaom.crazyauctions.reloaded.util.Category;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager.Files;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

public class GUICommand
    implements CrazyAuctionsSubCommand
{
    @Override
    public void execute(CommandSender sender, String subCommand, String... args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "Players-Only");
            return;
        }
        if (!PluginControl.hasCommandPermission(sender, "Gui", true)) return;
        Player player = (Player) sender;
        if (PluginControl.isWorldDisabled(player)) {
            MessageUtil.sendMessage(sender, "World-Disabled");
            return;
        }
        if (args.length == 1) {
            if (Files.CONFIG.getFile().getBoolean("Settings.Category-Page-Opens-First")) {
                GUI.openCategories(player, ShopType.ANY, Category.getDefaultCategory());
            } else {
                GUI.openShop(player, ShopType.ANY, Category.getDefaultCategory(), 1);
            }
        } else if (args.length == 2) {
            if (args[1].equalsIgnoreCase("sell")) {
                GUI.openShop(player, ShopType.SELL, Category.getDefaultCategory(), 1);
            } else if (args[1].equalsIgnoreCase("buy")) {
                GUI.openShop(player, ShopType.BUY, Category.getDefaultCategory(), 1);
            } else if (args[1].equalsIgnoreCase("bid")) {
                GUI.openShop(player, ShopType.BID, Category.getDefaultCategory(), 1);
            } else {
                GUI.openShop(player, ShopType.ANY, Category.getDefaultCategory(), 1);
            }
        } else if (args.length >= 3) {
            if (!PluginControl.hasCommandPermission(sender, "Gui-Others-Player", true)) return;
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                MessageUtil.sendMessage(sender, "Not-Online");
                return;
            }
            if (args[1].equalsIgnoreCase("sell")) {
                GUI.openShop(target, ShopType.SELL, Category.getDefaultCategory(), 1);
            } else if (args[1].equalsIgnoreCase("buy")) {
                GUI.openShop(target, ShopType.BUY, Category.getDefaultCategory(), 1);
            } else if (args[1].equalsIgnoreCase("bid")) {
                GUI.openShop(target, ShopType.BID, Category.getDefaultCategory(), 1);
            } else {
                GUI.openShop(target, ShopType.ANY, Category.getDefaultCategory(), 1);
            }
        }
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String subCommand, String... args) {
        if (args.length == 2) {
            return getTabElements(args, 2, Arrays.asList("sell", "buy", "bid"));
        }
        if (args.length == 3) {
            return getTabPlayersName(args, 3);
        }
        return new ArrayList<>();
    }

    @Override
    public CrazyAuctionsSubCommandType getCommandType() {
        return CrazyAuctionsSubCommandType.GUI;
    }
}
