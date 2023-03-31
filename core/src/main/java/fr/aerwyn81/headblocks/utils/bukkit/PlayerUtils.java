package fr.aerwyn81.headblocks.utils.bukkit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

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

    public static boolean hasEnoughInventorySpace(Player player, int spaceNeeded) {
        ItemStack[] items = player.getInventory().getStorageContents();

        int i = 0;
        for (ItemStack is : items) {
            if (is != null && is.getType() != Material.AIR)
                continue;
            i++;
        }

        return i < spaceNeeded;
    }

    public static int getFreeSlots(Player player, ItemStack[] itemsToIgnore) {
        return internalGetFreeSlots(player, itemsToIgnore);
    }

    public static int getFreeSlots(Player player) {
        return internalGetFreeSlots(player, null);
    }

    private static int internalGetFreeSlots(Player player, @Nullable ItemStack[] itemsToIgnore) {
        var i = 0;

        Inventory inv = player.getInventory();
        for (ItemStack item: inv.getStorageContents()) {
            if (item == null || (itemsToIgnore != null && Arrays.stream(itemsToIgnore).anyMatch(is -> is.isSimilar(item)))) {
                i++;
            }
        }

        return i;
    }
}
