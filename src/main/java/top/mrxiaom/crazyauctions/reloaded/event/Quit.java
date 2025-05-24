package top.mrxiaom.crazyauctions.reloaded.event;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import top.mrxiaom.crazyauctions.reloaded.database.Storage;

public class Quit 
    implements Listener
{
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Storage data = Storage.getPlayer(uuid);
        data.saveData();
    }
}
