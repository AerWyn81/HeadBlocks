package fr.aerwyn81.headblocks.utils.bukkit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class PlayerUtils {

    public static boolean hasPermission(CommandSender sender, String permission) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission(permission) || sender.isOp();
    }

    public static String getPseudoFromSession(String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            URLConnection request = url.openConnection();
            request.connect();

            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonObject();
            return jsonObject.get("name").getAsString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static int getEmptySlots(Player player) {
        int i = 0;

        ItemStack[] items = player.getInventory().getStorageContents();

        for (ItemStack is : items) {
            if (is != null && is.getType() != Material.AIR)
                continue;
            i++;
        }
        return i;
    }
}
