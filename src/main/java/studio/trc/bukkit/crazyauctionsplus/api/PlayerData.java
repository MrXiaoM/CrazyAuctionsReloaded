package studio.trc.bukkit.crazyauctionsplus.api;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import studio.trc.bukkit.crazyauctionsplus.database.Storage;

/**
 * This is just a handy guide.
 */
public class PlayerData
{
    public static Storage getPlayerData(Player player) {
        return Storage.getPlayer(player);
    }
    
    public static Storage getPlayerData(OfflinePlayer player) {
        return Storage.getPlayer(player);
    }
    
    public static Storage getPlayerData(UUID uuid) {
        return Storage.getPlayer(uuid);
    }
}
