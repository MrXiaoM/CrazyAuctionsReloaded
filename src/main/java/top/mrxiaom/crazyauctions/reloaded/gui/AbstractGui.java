package top.mrxiaom.crazyauctions.reloaded.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.crazyauctions.reloaded.Main;
import top.mrxiaom.crazyauctions.reloaded.database.GlobalMarket;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager;
import top.mrxiaom.crazyauctions.reloaded.util.PluginControl;

import static top.mrxiaom.crazyauctions.reloaded.gui.GUI.getFlag;

public abstract class AbstractGui implements IGui {
    protected final Main plugin = Main.getInstance();
    protected final FileManager.ProtectedConfiguration config = FileManager.Files.CONFIG.getFile();
    protected final Player player;
    protected Inventory inventory;

    public AbstractGui(Player player) {
        this.player = player;
    }

    protected void reopen() {
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        PluginControl.updateCacheData();
        run(this::open);
    }

    protected void run(Runnable runnable) {
        plugin.getScheduler().runTask(runnable);
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    protected Inventory create(int size, String title) {
        return Bukkit.createInventory(this, size, title);
    }
    @Override
    public Inventory newInventory() {
        inventory = null;
        createInventory();
        return inventory;
    }

    @Override
    public void onClick(InventoryAction action, ClickType click, InventoryType.SlotType slotType, int slot, ItemStack currentItem, ItemStack cursor, InventoryView view, InventoryClickEvent event) {
        event.setCancelled(true);
        if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
            player.closeInventory();
            return;
        }
        if (slot <= inventory.getSize() && currentItem != null) {
            String itemFlag = getFlag(currentItem);
            GlobalMarket market = GlobalMarket.getMarket();
            click(market, action, slot, currentItem, itemFlag, event);
        }
    }

    protected abstract void createInventory();
    protected abstract void click(
            GlobalMarket market, InventoryAction action,
            int slot, ItemStack item, String itemFlag,
            InventoryClickEvent e
    );
}
