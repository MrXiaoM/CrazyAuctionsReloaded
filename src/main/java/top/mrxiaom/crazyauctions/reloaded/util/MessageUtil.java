package top.mrxiaom.crazyauctions.reloaded.util;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.bukkit.command.CommandSender;

public class MessageUtil {

    /**
     * Send message to command sender.
     * @param sender Command sender.
     * @param path MessageUtil.yml's path
     */
    public static void sendMessage(CommandSender sender, String path) {
        if (sender == null) return;
        List<String> messages = FileManager.Files.MESSAGES.getFile().getStringList(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path);
        if (messages.isEmpty()) {
            sender.sendMessage(PluginControl.color(FileManager.Files.MESSAGES.getFile().getString(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path).replace("{prefix}", PluginControl.getPrefix()).replace("/n", "\n")));
        } else {
            for (String message : messages) {
                sender.sendMessage(PluginControl.color(message.replace("{prefix}", PluginControl.getPrefix()).replace("/n", "\n")));
            }
        }
    }
    
    /**
     * Send message to command sender.
     * @param sender Command sender.
     * @param path MessageUtil.yml's path
     * @param placeholders If the text contains a placeholder,
     *                      The placeholder will be replaced with the specified text.
     */
    public static void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) return;
        List<String> messages = FileManager.Files.MESSAGES.getFile().getStringList(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path);
        if (messages.isEmpty()) {
            String message = PluginControl.color(FileManager.Files.MESSAGES.getFile().getString(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path));
            for (String ph : placeholders.keySet()) {
                message = PluginControl.color(message.replaceAll(ph, placeholders.get(ph))).replaceAll(ph, placeholders.get(ph).toLowerCase());
            }
            sender.sendMessage(PluginControl.color(message.replace("{prefix}", PluginControl.getPrefix())).replace("/n", "\n"));
        } else {
            for (String message : messages) {
                for (String ph : placeholders.keySet()) {
                    message = PluginControl.color(message.replace(ph, placeholders.get(ph)).replace("{prefix}", PluginControl.getPrefix())).replace("/n", "\n");
                }
                sender.sendMessage(message);
            }
        }
    }
    
    /**
     * 
     * @param sender Command sender.
     * @param path MessageUtil.yml's path
     * @param placeholders If the text contains a placeholder,
     *                      The placeholder will be replaced with the specified text.
     * @param visible If the text contains a placeholder,
     *                 whether the entire line is visible or not will be
     *                 determined by the Boolean value corresponding to the placeholder.
     */
    public static void sendMessage(CommandSender sender, String path, Map<String, String> placeholders, Map<String, Boolean> visible) {
        if (sender == null) return;
        List<String> messages = FileManager.Files.MESSAGES.getFile().getStringList(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path);
        if (messages.isEmpty()) {
            String message = PluginControl.color(FileManager.Files.MESSAGES.getFile().getString(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path));
            for (String v : visible.keySet()) {
                if (message.contains(v)) {
                    if (!visible.get(v)) {
                        return;
                    }
                }
            }
            for (String ph : placeholders.keySet()) {
                message = PluginControl.color(message.replaceAll(ph, placeholders.get(ph))).replaceAll(ph, placeholders.get(ph).toLowerCase());
            }
            sender.sendMessage(PluginControl.color(message.replace("{prefix}", PluginControl.getPrefix())).replace("/n", "\n"));
        } else {
            for (String message : messages) {
                boolean isVisible = true;
                for (String v : visible.keySet()) {
                    if (message.contains(v)) {
                        if (!visible.get(v)) {
                            isVisible = false;
                            break;
                        } else {
                            message = message.replace(v, "");
                        }
                    }
                }
                if (!isVisible) {
                    continue;
                }
                for (String ph : placeholders.keySet()) {
                    message = PluginControl.color(message.replace(ph, placeholders.get(ph)).replace("{prefix}", PluginControl.getPrefix())).replace("/n", "\n");
                }
                sender.sendMessage(message);
            }
        }
    }
    
    public static String getValue(String path) {
        return PluginControl.color(FileManager.Files.MESSAGES.getFile().getString(FileManager.Files.CONFIG.getFile().getString("Settings.Language") + "." + path).replace("{prefix}", PluginControl.getPrefix()).replace("/n", "\n"));
    }
    
    public static List<String> getValueList(String path) {
        List<String> list = new ArrayList<>();
        FileManager.ProtectedConfiguration config = FileManager.Files.CONFIG.getFile();
        for (String message : FileManager.Files.MESSAGES.getFile().getStringList(config.getString("Settings.Language") + "." + path)) {
            list.add(PluginControl.color(message.replace("{prefix}", config.getString("Settings.Prefix"))));
        }
        return list;
    }
}
