package top.mrxiaom.crazyauctions.reloaded.command.subcommand;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.gui.GUI;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

public class ViewCommand
    implements CrazyAuctionsSubCommand
{
    @Override
    public void execute(CommandSender sender, String subCommand, String... args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "Players-Only");
            return;
        }
        Player player = (Player) sender;
        if (PluginControl.isWorldDisabled(player)) {
            MessageUtil.sendMessage(sender, "World-Disabled");
            return;
        }
        if (args.length == 1) {
            if (!PluginControl.hasCommandPermission(sender, "View", true)) return;
            GUI.openViewer(player, player.getUniqueId(), 0);
            return;
        }
        if (args.length >= 2) {
            if (!PluginControl.hasCommandPermission(sender, "View-Others-Player", true)) return;
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                GUI.openViewer(player, target.getUniqueId(), 1);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        GUI.openViewer(player, Bukkit.getOfflinePlayer(args[1]).getUniqueId(), 1);
                    }
                }.runTaskLater(Main.getInstance(), 1);
            }
            return;
        }
        MessageUtil.sendMessage(sender, "CrazyAuctions-View");
    }

    @Override
    public String getName() {
        return "view";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String subCommand, String... args) {
        if (args.length >= 2) {
            if (PluginControl.hasCommandPermission(sender, "View-Others-Player", false)) {
                return getTabPlayersName(args, args.length);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public CrazyAuctionsSubCommandType getCommandType() {
        return CrazyAuctionsSubCommandType.VIEW;
    }
}
