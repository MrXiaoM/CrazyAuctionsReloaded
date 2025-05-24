package top.mrxiaom.crazyauctions.reloaded.command.subcommand;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.gui.GUI;
import top.mrxiaom.crazyauctions.reloaded.util.Category;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.util.enums.ShopType;

public class MailCommand
    implements CrazyAuctionsSubCommand
{

    @Override
    public void execute(CommandSender sender, String subCommand, String... args) {
        if (!PluginControl.hasCommandPermission(sender, "Mail", true)) return;
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "Players-Only");
            return;
        }
        Player player = (Player) sender;
        if (PluginControl.isWorldDisabled(player)) {
            MessageUtil.sendMessage(sender, "World-Disabled");
            return;
        }
        GUI.openPlayersMail(player, ShopType.ANY, Category.getDefaultCategory(), 1);
    }

    @Override
    public String getName() {
        return "mail";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String subCommand, String... args) {
        return new ArrayList<>();
    }

    @Override
    public CrazyAuctionsSubCommandType getCommandType() {
        return CrazyAuctionsSubCommandType.MAIL;
    }
}
