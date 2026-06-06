package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.behavior.ZoneBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.CuboidZoneProvider;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.WorldGuardZoneProvider;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneMessageMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneProvider;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ZoneConfigGui {

    private enum Capture {
        CORNER1,
        CORNER2,
        RETURN
    }

    private final ServiceRegistry registry;

    private final ConcurrentHashMap<UUID, String> zoneTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> corner1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> corner2 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> returnPoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> wgRegions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> wgWorlds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Capture> pendingCaptures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> blockExits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> resetOnLeaves = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ZoneMessageMode> messageModes = new ConcurrentHashMap<>();
    private final Set<UUID> pendingChatInput = ConcurrentHashMap.newKeySet();
    private final Set<UUID> outlineViewers = ConcurrentHashMap.newKeySet();

    public ZoneConfigGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        zoneTypes.putIfAbsent(uuid, CuboidZoneProvider.TYPE);
        blockExits.putIfAbsent(uuid, false);
        resetOnLeaves.putIfAbsent(uuid, false);
        messageModes.putIfAbsent(uuid, ZoneMessageMode.CHAT);
        outlineViewers.add(uuid);
        buildAndOpenGui(player);
    }

    private void buildAndOpenGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.ZoneConfigTitle"), false, 4);

        IntStream.range(0, 36).forEach(index -> menu.setItem(0, index, borderItem()));

        UUID uuid = player.getUniqueId();
        boolean worldGuard = WorldGuardZoneProvider.TYPE.equals(zoneTypes.getOrDefault(uuid, CuboidZoneProvider.TYPE));
        boolean blockExit = blockExits.getOrDefault(uuid, false);

        // --- Required setup (row 1) ---

        // Slot 10: zone type toggle
        menu.setItem(0, 10, new ItemGUI(new ItemBuilder(worldGuard ? Material.MAP : Material.STRUCTURE_VOID)
                .setName(registry.getLanguageService().message("Gui.ZoneConfigType"))
                .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigTypeLore").stream()
                        .map(s -> s.replace("%type%", worldGuard
                                ? registry.getLanguageService().message("Gui.ZoneConfigTypeWorldGuard")
                                : registry.getLanguageService().message("Gui.ZoneConfigTypeCuboid")))
                        .collect(Collectors.toList()))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    zoneTypes.put(p.getUniqueId(), worldGuard ? CuboidZoneProvider.TYPE : WorldGuardZoneProvider.TYPE);
                    buildAndOpenGui(p);
                }));

        if (worldGuard) {
            buildWorldGuardItems(menu, uuid);
        } else {
            buildCuboidItems(menu, uuid);
        }

        // Slot 13: return point (only relevant when exit is blocked)
        if (blockExit) {
            Location rp = returnPoints.get(uuid);
            menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.ENDER_PEARL)
                    .setName(registry.getLanguageService().message("Gui.ZoneConfigReturnPoint"))
                    .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigReturnPointLore").stream()
                            .map(s -> s.replace("%location%", describePoint(rp)))
                            .collect(Collectors.toList()))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.RETURN)));
        }

        // --- Options (right column) ---

        // Slot 15: block exit toggle
        boolean reset = resetOnLeaves.getOrDefault(uuid, false);
        menu.setItem(0, 15, toggleItem("Gui.ZoneConfigBlockExit", "Gui.ZoneConfigBlockExitLore", blockExit)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    blockExits.put(p.getUniqueId(), !blockExit);
                    buildAndOpenGui(p);
                }));

        // Slot 24: reset on leave toggle
        menu.setItem(0, 24, toggleItem("Gui.ZoneConfigResetOnLeave", "Gui.ZoneConfigResetOnLeaveLore", reset)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    resetOnLeaves.put(p.getUniqueId(), !reset);
                    buildAndOpenGui(p);
                }));

        // Slot 16: message display mode
        ZoneMessageMode mode = messageModes.getOrDefault(uuid, ZoneMessageMode.CHAT);
        menu.setItem(0, 16, new ItemGUI(new ItemBuilder(Material.OAK_SIGN)
                .setName(registry.getLanguageService().message("Gui.ZoneConfigMessageMode"))
                .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigMessageModeLore").stream()
                        .map(s -> s.replace("%mode%", messageModeLabel(mode)))
                        .collect(Collectors.toList()))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    messageModes.put(p.getUniqueId(), mode.next());
                    buildAndOpenGui(p);
                }));

        // Slot 31: validate (row 3)
        buildValidateButton(menu, uuid);

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> {
                            Player p = (Player) event.getWhoClicked();
                            clearState(p.getUniqueId());
                            registry.getGuiService().getBehaviorSelectionManager().buildAndOpenGui(p);
                        });
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    private void buildCuboidItems(HBMenu menu, UUID uuid) {
        menu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.LIME_CONCRETE)
                .setName(registry.getLanguageService().message("Gui.ZoneConfigCorner1"))
                .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigCornerLore").stream()
                        .map(s -> s.replace("%location%", describe(corner1.get(uuid))))
                        .collect(Collectors.toList()))
                .toItemStack(), true)
                .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.CORNER1)));

        menu.setItem(0, 21, new ItemGUI(new ItemBuilder(Material.RED_CONCRETE)
                .setName(registry.getLanguageService().message("Gui.ZoneConfigCorner2"))
                .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigCornerLore").stream()
                        .map(s -> s.replace("%location%", describe(corner2.get(uuid))))
                        .collect(Collectors.toList()))
                .toItemStack(), true)
                .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.CORNER2)));
    }

    private void buildWorldGuardItems(HBMenu menu, UUID uuid) {
        String region = wgRegions.get(uuid);
        String regionText = region != null
                ? MessageUtils.colorize("&a" + region)
                : registry.getLanguageService().message("Gui.ZoneConfigNotDefined");

        menu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.NAME_TAG)
                .setName(registry.getLanguageService().message("Gui.ZoneConfigRegion"))
                .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigRegionLore").stream()
                        .map(s -> s.replace("%region%", regionText))
                        .collect(Collectors.toList()))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    wgWorlds.put(p.getUniqueId(), p.getWorld().getName());
                    pendingChatInput.add(p.getUniqueId());
                    p.sendMessage(registry.getLanguageService().message("Messages.ZoneRegionPrompt"));
                }));
    }

    private ItemGUI toggleItem(String nameKey, String loreKey, boolean enabled) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled
                ? registry.getLanguageService().message("Gui.StatusEnabled")
                : registry.getLanguageService().message("Gui.StatusDisabled");

        return new ItemGUI(new ItemBuilder(material)
                .setName(registry.getLanguageService().message(nameKey))
                .setLore(registry.getLanguageService().messageList(loreKey).stream()
                        .map(s -> s.replace("%status%", status))
                        .collect(Collectors.toList()))
                .toItemStack(), true);
    }

    private String messageModeLabel(ZoneMessageMode mode) {
        return switch (mode) {
            case ACTION_BAR -> registry.getLanguageService().message("Gui.ZoneConfigMessageModeActionBar");
            case TITLE -> registry.getLanguageService().message("Gui.ZoneConfigMessageModeTitle");
            default -> registry.getLanguageService().message("Gui.ZoneConfigMessageModeChat");
        };
    }

    private void buildValidateButton(HBMenu menu, UUID uuid) {
        if (isReady(uuid)) {
            menu.setItem(0, 31, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                    .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 31, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                    .setLore(registry.getLanguageService().messageList("Gui.ZoneConfigValidateBlockedLore"))
                    .toItemStack()));
        }
    }

    private ItemGUI borderItem() {
        return new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack());
    }

    private void beginCapture(Player player, Capture capture) {
        pendingCaptures.put(player.getUniqueId(), capture);
        player.closeInventory();
        String key = capture == Capture.RETURN ? "Messages.ZoneSneakPoint" : "Messages.ZonePlaceBlock";
        player.sendMessage(registry.getLanguageService().message(key));
    }

    public boolean isAwaitingBlockClick(UUID playerUuid) {
        Capture capture = pendingCaptures.get(playerUuid);
        return capture == Capture.CORNER1 || capture == Capture.CORNER2;
    }

    public boolean isAwaitingSneak(UUID playerUuid) {
        return pendingCaptures.get(playerUuid) == Capture.RETURN;
    }

    public void handleBlockClick(Player player, Location blockLocation) {
        UUID uuid = player.getUniqueId();
        Capture capture = pendingCaptures.get(uuid);
        if (capture != Capture.CORNER1 && capture != Capture.CORNER2) {
            return;
        }

        pendingCaptures.remove(uuid);
        if (capture == Capture.CORNER1) {
            corner1.put(uuid, blockLocation.clone());
        } else {
            corner2.put(uuid, blockLocation.clone());
        }

        player.sendMessage(registry.getLanguageService().message("Messages.ZonePositionSet"));
        open(player);
    }

    public void handleReturnSneak(Player player) {
        UUID uuid = player.getUniqueId();
        if (pendingCaptures.get(uuid) != Capture.RETURN) {
            return;
        }

        pendingCaptures.remove(uuid);
        Location loc = player.getLocation();
        returnPoints.put(uuid, new Location(loc.getWorld(),
                loc.getBlockX() + 0.5, loc.getY(), loc.getBlockZ() + 0.5,
                loc.getYaw(), loc.getPitch()));

        player.sendMessage(registry.getLanguageService().message("Messages.ZonePositionSet"));
        open(player);
    }

    public boolean hasPendingChatInput(Player player) {
        return pendingChatInput.contains(player.getUniqueId());
    }

    public void processPendingChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        if (!pendingChatInput.remove(uuid)) {
            return;
        }

        wgRegions.put(uuid, message.trim());
        player.sendMessage(registry.getLanguageService().message("Messages.ZoneRegionSet")
                .replace("%region%", message.trim()));
        open(player);
    }

    private boolean isReady(UUID uuid) {
        ZoneProvider provider = buildProvider(uuid);
        if (provider == null) {
            return false;
        }

        if (!blockExits.getOrDefault(uuid, false)) {
            return true;
        }

        Location rp = returnPoints.get(uuid);
        if (rp == null) {
            return false;
        }

        if (provider instanceof CuboidZoneProvider) {
            return provider.contains(rp);
        }
        if (provider.isAvailable()) {
            return provider.contains(rp);
        }
        return false;
    }

    private ZoneProvider buildProvider(UUID uuid) {
        String type = zoneTypes.getOrDefault(uuid, CuboidZoneProvider.TYPE);

        if (WorldGuardZoneProvider.TYPE.equals(type)) {
            String region = wgRegions.get(uuid);
            String world = wgWorlds.get(uuid);
            if (region == null || region.isEmpty() || world == null) {
                return null;
            }
            return new WorldGuardZoneProvider(world, region);
        }

        Location c1 = corner1.get(uuid);
        Location c2 = corner2.get(uuid);
        if (c1 == null || c2 == null || c1.getWorld() == null || c2.getWorld() == null) {
            return null;
        }
        if (!c1.getWorld().equals(c2.getWorld())) {
            return null;
        }

        return new CuboidZoneProvider(c1.getWorld().getName(),
                c1.getBlockX(), c1.getBlockY(), c1.getBlockZ(),
                c2.getBlockX(), c2.getBlockY(), c2.getBlockZ());
    }

    private void handleValidate(Player player) {
        UUID uuid = player.getUniqueId();
        ZoneProvider provider = buildProvider(uuid);
        boolean blockExit = blockExits.getOrDefault(uuid, false);
        Location rp = blockExit ? returnPoints.get(uuid) : null;
        boolean resetOnLeave = resetOnLeaves.getOrDefault(uuid, false);
        ZoneMessageMode messageMode = messageModes.getOrDefault(uuid, ZoneMessageMode.CHAT);

        registry.getGuiService().getBehaviorSelectionManager()
                .setPendingZone(uuid, new ZoneBehavior(registry, provider, rp, blockExit, resetOnLeave, messageMode));

        Set<String> selected = registry.getGuiService().getBehaviorSelectionManager().getSelectedBehaviors(uuid);
        clearState(uuid);

        if (selected != null && selected.contains("timed")) {
            registry.getGuiService().getTimedConfigManager().open(player);
            return;
        }
        if (selected != null && selected.contains("scheduled")) {
            registry.getGuiService().getScheduledConfigManager().open(player, null, true);
            return;
        }

        registry.getGuiService().getBehaviorSelectionManager().createHunt(player, null, true, null);
    }

    private String describe(Location location) {
        if (location == null) {
            return registry.getLanguageService().message("Gui.ZoneConfigNotDefined");
        }
        return MessageUtils.colorize(registry.getLanguageService().message("Gui.ZoneConfigLocation")
                .replace("%world%", location.getWorld() != null ? location.getWorld().getName() : "?")
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ())));
    }

    private String describePoint(Location location) {
        if (location == null) {
            return registry.getLanguageService().message("Gui.ZoneConfigNotDefined");
        }
        return MessageUtils.colorize(registry.getLanguageService().message("Gui.ZoneConfigLocation")
                .replace("%world%", location.getWorld() != null ? location.getWorld().getName() : "?")
                .replace("%x%", String.format(Locale.US, "%.1f", location.getX()))
                .replace("%y%", String.format(Locale.US, "%.1f", location.getY()))
                .replace("%z%", String.format(Locale.US, "%.1f", location.getZ())));
    }

    public boolean isOutlineViewer(UUID playerUuid) {
        return outlineViewers.contains(playerUuid);
    }

    public boolean isAwaitingCapture(UUID playerUuid) {
        return pendingCaptures.containsKey(playerUuid);
    }

    public void renderOutlines() {
        for (UUID uuid : outlineViewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            renderOutline(player, uuid);
        }
    }

    private void renderOutline(Player player, UUID uuid) {
        if (WorldGuardZoneProvider.TYPE.equals(zoneTypes.getOrDefault(uuid, CuboidZoneProvider.TYPE))) {
            ZoneProvider provider = buildProvider(uuid);
            if (!(provider instanceof WorldGuardZoneProvider wg) || !wg.isAvailable()) {
                return;
            }
            int[] b = wg.getBounds();
            World world = Bukkit.getWorld(wg.getWorldName());
            if (b == null || world == null) {
                return;
            }
            drawBox(player, world, b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
            return;
        }

        Location c1 = corner1.get(uuid);
        Location c2 = corner2.get(uuid);
        if (c1 == null || c2 == null || c1.getWorld() == null || !c1.getWorld().equals(c2.getWorld())) {
            return;
        }

        double minX = Math.min(c1.getBlockX(), c2.getBlockX());
        double minY = Math.min(c1.getBlockY(), c2.getBlockY());
        double minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        double maxX = Math.max(c1.getBlockX(), c2.getBlockX()) + 1;
        double maxY = Math.max(c1.getBlockY(), c2.getBlockY()) + 1;
        double maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ()) + 1;
        drawBox(player, c1.getWorld(), minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void drawBox(Player player, World world, double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) {
        double[][] edges = {
                {minX, minY, minZ, maxX, minY, minZ}, {minX, minY, maxZ, maxX, minY, maxZ},
                {minX, maxY, minZ, maxX, maxY, minZ}, {minX, maxY, maxZ, maxX, maxY, maxZ},
                {minX, minY, minZ, minX, maxY, minZ}, {maxX, minY, minZ, maxX, maxY, minZ},
                {minX, minY, maxZ, minX, maxY, maxZ}, {maxX, minY, maxZ, maxX, maxY, maxZ},
                {minX, minY, minZ, minX, minY, maxZ}, {maxX, minY, minZ, maxX, minY, maxZ},
                {minX, maxY, minZ, minX, maxY, maxZ}, {maxX, maxY, minZ, maxX, maxY, maxZ}
        };

        for (double[] e : edges) {
            double length = Math.max(Math.abs(e[3] - e[0]), Math.max(Math.abs(e[4] - e[1]), Math.abs(e[5] - e[2])));
            int steps = Math.max(1, (int) Math.min(length, 16));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = e[0] + (e[3] - e[0]) * t;
                double y = e[1] + (e[4] - e[1]) * t;
                double z = e[2] + (e[5] - e[2]) * t;
                player.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public void clearState(UUID playerUuid) {
        zoneTypes.remove(playerUuid);
        corner1.remove(playerUuid);
        corner2.remove(playerUuid);
        returnPoints.remove(playerUuid);
        wgRegions.remove(playerUuid);
        wgWorlds.remove(playerUuid);
        pendingCaptures.remove(playerUuid);
        pendingChatInput.remove(playerUuid);
        outlineViewers.remove(playerUuid);
        blockExits.remove(playerUuid);
        resetOnLeaves.remove(playerUuid);
        messageModes.remove(playerUuid);
    }
}
