package fr.aerwyn81.headblocks.data.reward;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Reward {
    private final RewardType type;
    private final String value;

    public Reward(RewardType type, String value) {
        this.type = type;
        this.value = value;
    }

    public RewardType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public HashMap<String, String> serialize() {
        var map = new HashMap<String, String>();
        map.put("type", type.name());
        map.put("value", value);

        return map;
    }

    public static Reward deserialize(Object object) {
        try {
            var type = RewardType.UNKNOWN;
            var value = "";

            var map = (LinkedHashMap<?, ?>) object;
            for (var entry : map.entrySet()) {
                if (entry.getKey().equals("type"))
                    type = RewardType.valueOf(entry.getValue().toString().toUpperCase());

                if (entry.getKey().equals("value"))
                    value = entry.getValue().toString();
            }

            return new Reward(type, value);
        } catch (Exception ex) {
            LogUtil.error("Error while deserializing reward: {0}", ex.getMessage());
            return new Reward(RewardType.UNKNOWN, "");
        }
    }

    public void execute(Player player, HeadLocation headLocation) {
        var plugin = HeadBlocks.getInstance();

        var parsedValue = "";
        try {
            parsedValue = PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation, value);
        } catch (Exception ex) {
            LogUtil.error("Error executing head reward \"{0}\": {1}", value, ex.getMessage());
        }

        if (parsedValue.isEmpty())
            return;

        var value = parsedValue;
        switch (type) {
            case MESSAGE -> player.sendMessage(parsedValue);
            case COMMAND -> Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), value), 1L);
            case BROADCAST -> plugin.getServer().broadcastMessage(parsedValue);
        }
    }
}
