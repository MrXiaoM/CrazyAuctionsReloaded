package studio.trc.bukkit.crazyauctionsplus.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import studio.trc.bukkit.crazyauctionsplus.util.PluginControl;

public class Teleport
    implements Listener
{
    @EventHandler(priority = EventPriority.LOWEST)
    public void teleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        if (PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
        }
    }
}
