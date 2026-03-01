package fr.aerwyn81.headblocks.data.reward;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;

public record Reward(RewardType type, String value) {

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
                if (entry.getKey().equals("type")) {
                    type = RewardType.valueOf(entry.getValue().toString().toUpperCase());
                }

                if (entry.getKey().equals("value")) {
                    value = entry.getValue().toString();
                }
            }

            return new Reward(type, value);
        } catch (Exception ex) {
            LogUtil.error("Error while deserializing reward: {0}", ex.getMessage());
            return new Reward(RewardType.UNKNOWN, "");
        }
    }

    public void execute(Player player, HeadLocation headLocation, ServiceRegistry registry) {
        var parsedValue = "";
        try {
            parsedValue = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), headLocation, value);
        } catch (Exception ex) {
            LogUtil.error("Error executing head reward \"{0}\": {1}", value, ex.getMessage());
        }

        if (parsedValue.isEmpty()) {
            return;
        }

        var val = parsedValue;
        switch (type) {
            case MESSAGE -> player.sendMessage(parsedValue);
            case COMMAND -> registry.getScheduler().runTaskLater(() ->
                    registry.getCommandDispatcher().dispatchConsoleCommand(val), 1L);
            case BROADCAST -> registry.getPluginProvider().getJavaPlugin().getServer().broadcastMessage(parsedValue);
        }
    }
}
