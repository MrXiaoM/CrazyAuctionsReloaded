package studio.trc.bukkit.crazyauctionsplus.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import studio.trc.bukkit.crazyauctionsplus.util.FileManager.*;

public class EasyCommand
    implements Listener
{
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void command(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        ProtectedConfiguration config = Files.CONFIG.getFile();
        String command = e.getMessage().substring(1);
        if (config.getBoolean("Settings.Easy-Commands.Enabled")) {
            for (String commands : config.getConfigurationSection("Settings.Easy-Commands.Commands").getKeys(false)) {
                if (command.equalsIgnoreCase(commands)) {
                    p.performCommand(config.getString("Settings.Easy-Commands.Commands." + commands));
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
