package top.mrxiaom.crazyauctions.reloaded.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;
import top.mrxiaom.crazyauctions.reloaded.util.FileManager;

public class ItemCollection
{
    private final ItemStack is;
    private final String displayName;
    private final long uid;
    
    public ItemCollection(ItemStack is, long uid, String displayName) {
        this.is = is;
        this.uid = uid;
        this.displayName = displayName;
    }
    
    public ItemStack getItem() {
        return is;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public long getUID() {
        return uid;
    }
    
    public static boolean addItem(ItemStack item, String displayName) {
        if (displayName == null) return false;
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.get("ItemCollection." + items + ".Item") != null) {
                    ItemMeta meta = ic.getItemStack("ItemCollection." + items + ".Item").getItemMeta();
                    if (Objects.equals(item.getItemMeta(), meta)) {
                        return false;
                    }
                }
            }
            if (ic.get("ItemCollection." + displayName) != null) {
                return false;
            } else {
                long uid = makeUID();
                ic.set("ItemCollection." + displayName + ".UID", uid);
                ic.set("ItemCollection." + displayName + ".Item", item);
                FileManager.Files.ITEM_COLLECTION.saveFile();
                return true;
            }
        } else {
            ic.set("ItemCollection." + displayName + ".UID", 1L);
            ic.set("ItemCollection." + displayName + ".Item", item);
            FileManager.Files.ITEM_COLLECTION.saveFile();
            return true;
        }
    }
    
    public static void deleteItem(long uid) {
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.getLong("ItemCollection." + items + ".UID") == uid) {
                    ic.set("ItemCollection." + items, null);
                    FileManager.Files.ITEM_COLLECTION.saveFile();
                    return;
                }
            }
        }
    }
    
    public static void deleteItem(String displayName) {
        if (displayName == null) return;
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (items.equalsIgnoreCase(displayName)) {
                    ic.set("ItemCollection." + items, null);
                    FileManager.Files.ITEM_COLLECTION.saveFile();
                    return;
                }
            }
        }
    }
    
    public static long makeUID() {
        long id = 0;
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        while (true) {
            id++;
            boolean b = false;
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.get("ItemCollection." + items + ".Item") != null) {
                    if (ic.getLong("ItemCollection." + items + ".UID") == id) {
                        b = true;
                        break;
                    }
                }
            }
            if (b) continue;
            break;
        }
        return id;
    }
    
    public static List<ItemCollection> getCollection() {
        List<ItemCollection> list = new ArrayList<>();
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.get("ItemCollection." + items + ".Item") != null) {
                    list.add(new ItemCollection(ic.getItemStack("ItemCollection." + items + ".Item"), ic.getLong("ItemCollection." + items + ".UID"), items));
                }
            }
            return list;
        }
        return list;
    }
    
    public static ItemCollection getItemCollection(long uid) {
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.get("ItemCollection." + items + ".Item") != null) {
                    if (ic.getLong("ItemCollection." + items + ".UID") == uid) {
                        return new ItemCollection(ic.getItemStack("ItemCollection." + items + ".Item"), ic.getLong("ItemCollection." + items + ".UID"), items);
                    }
                }
            }
        }
        return null;
    }
    
    public static ItemCollection getItemCollection(String displayName) {
        FileManager.ProtectedConfiguration ic = FileManager.Files.ITEM_COLLECTION.getFile();
        if (ic.get("ItemCollection") != null) {
            for (String items : ic.getConfigurationSection("ItemCollection").getKeys(false)) {
                if (ic.get("ItemCollection." + items + ".UID") != null && ic.get("ItemCollection." + items + ".Item") != null) {
                    if (displayName.equalsIgnoreCase(items)) {
                        return new ItemCollection(ic.getItemStack("ItemCollection." + items + ".Item"), ic.getLong("ItemCollection." + items + ".UID"), items);
                    }
                }
            }
        }
        return null;
    }
}
