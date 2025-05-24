package top.mrxiaom.crazyauctions.reloaded.event;

import org.bukkit.Bukkit;
import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.util.MessageUtil;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager.Files;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;
import top.mrxiaom.crazyauctions.reloaded.database.Storage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Join implements Listener {
    private final Main plugin;
    public Join(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            return;
        }
        if (!Files.CONFIG.getFile().getBoolean("Settings.Join-Message")) return;
        plugin.getScheduler().runTaskLaterAsync(() -> {
            Storage data = Storage.getPlayer(player);
            if (data.getMailNumber() > 0) {
                MessageUtil.sendMessage(player, "Email-of-player-owned-items");
            }
        }, 2000L);
    }
}
