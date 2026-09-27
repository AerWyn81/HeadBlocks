package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FixedPositionBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnDraft;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnPoint;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@HBAnnotations(command = "spawn", permission = "headblocks.admin", args = {"hunt"})
public class Spawn implements Cmd {
    private static final int TARGET_DISTANCE = 10;
    private static final int SHOW_SECONDS = 15;

    private final ServiceRegistry registry;

    public Spawn(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnUsage"));
            return true;
        }

        var hunt = registry.getHuntService().getHuntById(args[1].toLowerCase());
        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replace("%hunt%", args[1]));
            return true;
        }

        var behavior = fixedOf(hunt);
        if (behavior == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnNotFixedPosition")
                    .replace("%hunt%", hunt.getId()));
            return true;
        }

        switch (args[2].toLowerCase()) {
            case "point" -> handlePoint(sender, hunt, behavior, args);
            case "config" -> openConfig(sender, hunt, behavior);
            case "reroll" -> {
                boolean reset = args.length > 3 && args[3].equalsIgnoreCase("reset");
                registry.getSpawnService().reroll(hunt, reset);
                sender.sendMessage(registry.getLanguageService().message("Messages.SpawnRerolled")
                        .replace("%hunt%", hunt.getId()));
            }
            case "clear" -> {
                registry.getSpawnService().clear(hunt);
                sender.sendMessage(registry.getLanguageService().message("Messages.SpawnCleared")
                        .replace("%hunt%", hunt.getId()));
            }
            default -> sender.sendMessage(registry.getLanguageService().message("Messages.SpawnUsage"));
        }
        return true;
    }

    private void openConfig(CommandSender sender, HBHunt hunt, FixedPositionBehavior behavior) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        registry.getGuiService().getSpawnConfigGui().open(player, SpawnDraft.of(behavior),
                draft -> {
                    player.closeInventory();
                    applyConfig(player, hunt, draft);
                },
                Player::closeInventory);
    }

    void applyConfig(Player player, HBHunt hunt, SpawnDraft draft) {
        var current = fixedOf(hunt);
        if (current == null) {
            return;
        }

        draft.points = new ArrayList<>(current.points());
        var behaviors = new ArrayList<Behavior>(hunt.getBehaviors());
        behaviors.replaceAll(behavior -> behavior == current ? draft.build(registry) : behavior);
        hunt.setBehaviors(behaviors);

        registry.getHuntConfigService().saveHunt(hunt);
        registry.getStorageService().incrementHuntVersion();
        registry.getSpawnService().reconfigure(hunt);

        player.sendMessage(registry.getLanguageService().message("Messages.SpawnConfigSaved")
                .replace("%hunt%", hunt.getId()));
    }

    private void handlePoint(CommandSender sender, HBHunt hunt, FixedPositionBehavior behavior, String[] args) {
        var action = args.length > 3 ? args[3].toLowerCase() : "";
        switch (action) {
            case "add" -> addPoint(sender, hunt, behavior);
            case "remove" -> removePoint(sender, hunt, behavior, args);
            case "list" -> listPoints(sender, hunt, behavior);
            case "show" -> showPoints(sender, behavior);
            default -> sender.sendMessage(registry.getLanguageService().message("Messages.SpawnUsage"));
        }
    }

    private void addPoint(CommandSender sender, HBHunt hunt, FixedPositionBehavior behavior) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        var target = targetedSpot(player);
        if (target == null) {
            player.sendMessage(registry.getLanguageService().message("Messages.SpawnNoTarget"));
            return;
        }

        if (behavior.points().stream().anyMatch(point -> point.matches(target))) {
            player.sendMessage(registry.getLanguageService().message("Messages.SpawnPointExists"));
            return;
        }

        if (registry.getAreaEnforcementService().isLocationOutsideArea(hunt, target)) {
            player.sendMessage(registry.getLanguageService().message("Messages.AreaHeadOutside")
                    .replace("%hunt%", hunt.getDisplayName()));
            return;
        }

        var points = new ArrayList<>(behavior.points());
        points.add(SpawnPoint.of(target, HeadUtils.snapYaw(player.getLocation().getYaw() + 180f)));
        replace(hunt, behavior, points);

        player.sendMessage(registry.getLanguageService().message("Messages.SpawnPointAdded")
                .replace("%index%", String.valueOf(points.size()))
                .replace("%hunt%", hunt.getId()));
    }

    private void removePoint(CommandSender sender, HBHunt hunt, FixedPositionBehavior behavior, String[] args) {
        var points = new ArrayList<>(behavior.points());
        int index = -1;

        if (args.length > 4) {
            try {
                index = Integer.parseInt(args[4]) - 1;
            } catch (NumberFormatException ignored) {
            }
        } else if (sender instanceof Player player) {
            var target = targetedSpot(player);
            index = target == null ? -1 : IntStream.range(0, points.size())
                    .filter(i -> points.get(i).matches(target) || points.get(i).matches(target.clone().add(0, -1, 0)))
                    .findFirst().orElse(-1);
        }

        if (index < 0 || index >= points.size()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnPointNotFound"));
            return;
        }

        points.remove(index);
        replace(hunt, behavior, points);

        sender.sendMessage(registry.getLanguageService().message("Messages.SpawnPointRemoved")
                .replace("%index%", String.valueOf(index + 1))
                .replace("%hunt%", hunt.getId()));
    }

    private void listPoints(CommandSender sender, HBHunt hunt, FixedPositionBehavior behavior) {
        sender.sendMessage(registry.getLanguageService().message("Messages.SpawnPointListHeader")
                .replace("%hunt%", hunt.getId())
                .replace("%count%", String.valueOf(behavior.points().size())));

        var points = behavior.points();
        for (int i = 0; i < points.size(); i++) {
            var point = points.get(i);
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnPointListLine")
                    .replace("%index%", String.valueOf(i + 1))
                    .replace("%world%", point.world())
                    .replace("%x%", String.valueOf(point.x()))
                    .replace("%y%", String.valueOf(point.y()))
                    .replace("%z%", String.valueOf(point.z())));
        }
    }

    private void showPoints(CommandSender sender, FixedPositionBehavior behavior) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        var locations = behavior.points().stream().map(SpawnPoint::toLocation)
                .filter(location -> location != null && location.getWorld() == player.getWorld())
                .map(location -> location.clone().add(0, 0.5, 0))
                .toList();

        var particle = ParticlesUtils.resolve("HAPPY_VILLAGER");
        var runs = new AtomicInteger();
        var task = new AtomicReference<Task>();
        task.set(registry.getScheduler().runTaskTimer(player.getLocation(), () -> {
            if (!player.isOnline() || runs.incrementAndGet() > SHOW_SECONDS) {
                task.get().cancel();
                return;
            }
            locations.forEach(location -> ParticlesUtils.spawn(location, particle, 10, null, player));
        }, 1L, 20L));

        player.sendMessage(registry.getLanguageService().message("Messages.SpawnPointsShown")
                .replace("%count%", String.valueOf(locations.size())));
    }

    private Location targetedSpot(Player player) {
        var block = player.getTargetBlockExact(TARGET_DISTANCE);
        if (block == null || block.isEmpty()) {
            return null;
        }

        return block.getRelative(BlockFace.UP).getLocation();
    }

    private void replace(HBHunt hunt, FixedPositionBehavior behavior, List<SpawnPoint> points) {
        var behaviors = new ArrayList<Behavior>(hunt.getBehaviors());
        behaviors.replaceAll(current -> current == behavior ? behavior.withPoints(points) : current);
        hunt.setBehaviors(behaviors);

        registry.getHuntConfigService().saveHunt(hunt);
        registry.getStorageService().incrementHuntVersion();
        registry.getAreaEnforcementService().sanitizeAreaHunts();
        registry.getSpawnService().refresh(hunt);
    }

    private static FixedPositionBehavior fixedOf(HBHunt hunt) {
        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof FixedPositionBehavior fixed) {
                return fixed;
            }
        }
        return null;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        List<String> options = switch (args.length) {
            case 2 -> registry.getHuntService().getAllHunts().stream()
                    .filter(hunt -> fixedOf(hunt) != null)
                    .map(HBHunt::getId)
                    .toList();
            case 3 -> List.of("point", "config", "reroll", "clear");
            case 4 -> switch (args[2].toLowerCase()) {
                case "point" -> List.of("add", "remove", "list", "show");
                case "reroll" -> List.of("reset");
                default -> List.of();
            };
            default -> List.of();
        };

        return new ArrayList<>(options.stream().filter(option -> option.startsWith(args[args.length - 1].toLowerCase())).toList());
    }
}
