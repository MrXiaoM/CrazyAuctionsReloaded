package top.mrxiaom.crazyauctions.reloaded.command.subcommand;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommand;
import top.mrxiaom.crazyauctions.reloaded.command.CrazyAuctionsSubCommandType;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

public class HelpCommand
    implements CrazyAuctionsSubCommand
{
    @Override
    public void execute(CommandSender sender, String subCommand, String... args) {
        if (!PluginControl.hasCommandPermission(sender, "Help", true)) return;
        MessageUtil.sendMessage(sender, "Help-Menu");
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public CrazyAuctionsSubCommandType getCommandType() {
        return CrazyAuctionsSubCommandType.HELP;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String subCommand, String... args) {
        return new ArrayList<>();
    }
}
