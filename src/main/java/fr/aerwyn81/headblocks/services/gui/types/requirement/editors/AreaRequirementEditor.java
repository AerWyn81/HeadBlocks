package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.WorldGuardAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.services.gui.types.requirement.AbstractRequirementEditor;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * Editor of the area: its shape, and what happens to a player once they are inside.
 */
public class AreaRequirementEditor extends AbstractRequirementEditor {
    private static final int ROWS = 4;

    private enum Capture {
        CORNER1,
        CORNER2,
        RETURN
    }

    private static final class Draft {
        private String areaType = CuboidAreaProvider.TYPE;
        private Location corner1;
        private Location corner2;
        private Location returnPoint;
        private String wgRegion;
        private String wgWorld;
        private Capture pendingCapture;
        private boolean blockExit;
        private boolean resetOnLeave;
        private boolean outlining;
        private AreaMessageMode messageMode = AreaMessageMode.CHAT;

        private boolean isWorldGuard() {
            return WorldGuardAreaProvider.TYPE.equals(areaType);
        }
    }

    private final Map<UUID, Draft> drafts = new ConcurrentHashMap<>();

    public AreaRequirementEditor(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public RequirementType getType() {
        return RequirementType.AREA;
    }

    @Override
    public void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        rememberCallbacks(player, onDone, onCancel);
        seed(player.getUniqueId(), existing instanceof AreaRequirement area ? area : null);
        reopen(player);
    }

    private void seed(UUID uuid, AreaRequirement existing) {
        Draft draft = new Draft();
        drafts.put(uuid, draft);

        if (existing == null) {
            return;
        }

        draft.blockExit = existing.blockExit();
        draft.resetOnLeave = existing.resetOnLeave();
        draft.messageMode = existing.messageMode();
        draft.returnPoint = existing.returnPoint();

        AreaProvider area = existing.area();
        if (area instanceof WorldGuardAreaProvider wg) {
            draft.areaType = WorldGuardAreaProvider.TYPE;
            draft.wgRegion = wg.getRegionId();
            draft.wgWorld = wg.getWorldName();
        } else if (area instanceof CuboidAreaProvider cuboid) {
            World world = Bukkit.getWorld(cuboid.getWorldName());
            if (world != null) {
                draft.corner1 = new Location(world, cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ());
                draft.corner2 = new Location(world, cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ());
            }
        }
    }

    private Draft draft(UUID uuid) {
        return drafts.computeIfAbsent(uuid, id -> new Draft());
    }

    private void reopen(Player player) {
        UUID uuid = player.getUniqueId();
        Draft draft = draft(uuid);
        draft.outlining = true;

        var menu = newMenu("Gui.AreaConfigTitle", ROWS);
        fillBorders(menu, ROWS);

        boolean worldGuard = draft.isWorldGuard();
        String typeLabel = registry.getLanguageService().message(worldGuard
                ? "Gui.AreaConfigTypeWorldGuard"
                : "Gui.AreaConfigTypeCuboid");

        menu.setItem(0, 10, fieldItem(worldGuard ? Material.MAP : Material.STRUCTURE_VOID,
                "Gui.AreaConfigType", "Gui.AreaConfigTypeLore", "%type%", typeLabel)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    draft(p.getUniqueId()).areaType = worldGuard
                            ? CuboidAreaProvider.TYPE
                            : WorldGuardAreaProvider.TYPE;
                    reopen(p);
                }));

        if (worldGuard) {
            buildWorldGuardItems(menu, draft);
        } else {
            buildCuboidItems(menu, draft);
        }

        if (draft.blockExit) {
            menu.setItem(0, 13, fieldItem(Material.ENDER_PEARL,
                    "Gui.AreaConfigReturnPoint", "Gui.AreaConfigReturnPointLore", "%location%",
                    describe(draft.returnPoint, true))
                    .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.RETURN)));
        }

        menu.setItem(0, 15, toggleItem("Gui.AreaConfigBlockExit", "Gui.AreaConfigBlockExitLore", draft.blockExit)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    draft(p.getUniqueId()).blockExit = !draft.blockExit;
                    reopen(p);
                }));

        menu.setItem(0, 24, toggleItem("Gui.AreaConfigResetOnLeave", "Gui.AreaConfigResetOnLeaveLore", draft.resetOnLeave)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    draft(p.getUniqueId()).resetOnLeave = !draft.resetOnLeave;
                    reopen(p);
                }));

        menu.setItem(0, 16, fieldItem(Material.OAK_SIGN,
                "Gui.AreaConfigMessageMode", "Gui.AreaConfigMessageModeLore", "%mode%",
                draft.messageMode.getLocalizedName(registry.getLanguageService()))
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    draft(p.getUniqueId()).messageMode = draft.messageMode.next();
                    reopen(p);
                }));

        menu.setItem(0, 31, isReady(draft)
                ? validateItem(() -> handleValidate(player))
                : blockedValidateItem("Gui.AreaConfigValidateBlockedLore"));

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    private void buildCuboidItems(HBMenu menu, Draft draft) {
        menu.setItem(0, 12, fieldItem(Material.LIME_CONCRETE,
                "Gui.AreaConfigCorner1", "Gui.AreaConfigCornerLore", "%location%", describe(draft.corner1, false))
                .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.CORNER1)));

        menu.setItem(0, 21, fieldItem(Material.RED_CONCRETE,
                "Gui.AreaConfigCorner2", "Gui.AreaConfigCornerLore", "%location%", describe(draft.corner2, false))
                .addOnClickEvent(event -> beginCapture((Player) event.getWhoClicked(), Capture.CORNER2)));
    }

    private void buildWorldGuardItems(HBMenu menu, Draft draft) {
        String regionText = draft.wgRegion != null ? MessageUtils.colorize("&a" + draft.wgRegion) : notDefined();

        menu.setItem(0, 12, fieldItem(Material.NAME_TAG,
                "Gui.AreaConfigRegion", "Gui.AreaConfigRegionLore", "%region%", regionText)
                .addOnClickEvent(event -> promptRegion((Player) event.getWhoClicked())));
    }

    private void promptRegion(Player player) {
        Draft draft = draft(player.getUniqueId());

        registry.getChatPromptService().prompt(player,
                registry.getLanguageService().message("Messages.AreaRegionPrompt"),
                input -> {
                    draft.wgRegion = input;

                    if (draft.wgWorld == null) {
                        draft.wgWorld = player.getWorld().getName();
                    }

                    player.sendMessage(registry.getLanguageService().message("Messages.AreaRegionSet")
                            .replace("%region%", input));
                    reopen(player);
                },
                this::reopen);
    }

    private ItemGUI toggleItem(String nameKey, String loreKey, boolean enabled) {
        String status = enabled
                ? registry.getLanguageService().message("Gui.StatusEnabled")
                : registry.getLanguageService().message("Gui.StatusDisabled");

        return fieldItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE, nameKey, loreKey, "%status%", status);
    }

    // --- World captures ---

    private void beginCapture(Player player, Capture capture) {
        draft(player.getUniqueId()).pendingCapture = capture;
        player.closeInventory();
        String key = capture == Capture.RETURN ? "Messages.AreaSneakPoint" : "Messages.AreaPlaceBlock";
        player.sendMessage(registry.getLanguageService().message(key));
    }

    private Capture pendingCapture(UUID playerUuid) {
        Draft draft = drafts.get(playerUuid);
        return draft == null ? null : draft.pendingCapture;
    }

    public boolean isAwaitingBlockClick(UUID playerUuid) {
        Capture capture = pendingCapture(playerUuid);
        return capture == Capture.CORNER1 || capture == Capture.CORNER2;
    }

    public boolean isAwaitingSneak(UUID playerUuid) {
        return pendingCapture(playerUuid) == Capture.RETURN;
    }

    public boolean isAwaitingCapture(UUID playerUuid) {
        return pendingCapture(playerUuid) != null;
    }

    public void handleBlockClick(Player player, Location blockLocation) {
        UUID uuid = player.getUniqueId();
        Draft draft = drafts.get(uuid);
        if (draft == null || (draft.pendingCapture != Capture.CORNER1 && draft.pendingCapture != Capture.CORNER2)) {
            return;
        }

        if (draft.pendingCapture == Capture.CORNER1) {
            draft.corner1 = blockLocation.clone();
        } else {
            draft.corner2 = blockLocation.clone();
        }
        draft.pendingCapture = null;

        player.sendMessage(registry.getLanguageService().message("Messages.AreaPositionSet"));
        reopen(player);
    }

    public void handleReturnSneak(Player player) {
        UUID uuid = player.getUniqueId();
        Draft draft = drafts.get(uuid);
        if (draft == null || draft.pendingCapture != Capture.RETURN) {
            return;
        }

        draft.pendingCapture = null;
        Location loc = player.getLocation();
        draft.returnPoint = new Location(loc.getWorld(),
                loc.getBlockX() + 0.5, loc.getY(), loc.getBlockZ() + 0.5,
                loc.getYaw(), loc.getPitch());

        player.sendMessage(registry.getLanguageService().message("Messages.AreaPositionSet"));
        reopen(player);
    }

    // --- Validation ---

    private boolean isReady(Draft draft) {
        AreaProvider provider = buildProvider(draft);
        if (provider == null) {
            return false;
        }

        if (!draft.blockExit) {
            return true;
        }

        Location rp = draft.returnPoint;
        if (rp == null) {
            return false;
        }

        return (provider instanceof CuboidAreaProvider || provider.isAvailable()) && provider.contains(rp);
    }

    private AreaProvider buildProvider(Draft draft) {
        if (draft.isWorldGuard()) {
            if (draft.wgRegion == null || draft.wgRegion.isEmpty() || draft.wgWorld == null) {
                return null;
            }
            return new WorldGuardAreaProvider(draft.wgWorld, draft.wgRegion);
        }

        Location c1 = draft.corner1;
        Location c2 = draft.corner2;
        if (c1 == null || c2 == null || c1.getWorld() == null || c2.getWorld() == null) {
            return null;
        }
        if (!c1.getWorld().equals(c2.getWorld())) {
            return null;
        }

        return new CuboidAreaProvider(c1.getWorld().getName(),
                c1.getBlockX(), c1.getBlockY(), c1.getBlockZ(),
                c2.getBlockX(), c2.getBlockY(), c2.getBlockZ());
    }

    private void handleValidate(Player player) {
        Draft draft = draft(player.getUniqueId());
        AreaProvider provider = buildProvider(draft);

        finish(player, new AreaRequirement(registry, provider, draft.returnPoint,
                draft.blockExit, draft.resetOnLeave, draft.messageMode));
    }

    // --- Outline rendering ---

    public boolean isOutlineViewer(UUID playerUuid) {
        Draft draft = drafts.get(playerUuid);
        return draft != null && draft.outlining;
    }

    public void renderOutlines() {
        for (Map.Entry<UUID, Draft> entry : drafts.entrySet()) {
            Draft draft = entry.getValue();
            if (!draft.outlining) {
                continue;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }

            registry.getScheduler().runNow(player, () -> renderOutline(player, draft));
        }
    }

    private void renderOutline(Player player, Draft draft) {
        if (draft.isWorldGuard()) {
            if (!(buildProvider(draft) instanceof WorldGuardAreaProvider wg)) {
                return;
            }

            int[] b = wg.getBounds();
            World world = Bukkit.getWorld(wg.getWorldName());
            if (b == null || world == null) {
                return;
            }

            drawBox(player, b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
            return;
        }

        Location c1 = draft.corner1;
        Location c2 = draft.corner2;
        if (c1 == null || c2 == null || c1.getWorld() == null || !c1.getWorld().equals(c2.getWorld())) {
            return;
        }

        double minX = Math.min(c1.getBlockX(), c2.getBlockX());
        double minY = Math.min(c1.getBlockY(), c2.getBlockY());
        double minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        double maxX = Math.max(c1.getBlockX(), c2.getBlockX()) + 1;
        double maxY = Math.max(c1.getBlockY(), c2.getBlockY()) + 1;
        double maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ()) + 1;
        drawBox(player, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void drawBox(Player player, double minX, double minY, double minZ,
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

    // --- Rendering helpers ---

    private String describe(Location location, boolean precise) {
        if (location == null) {
            return notDefined();
        }

        DoubleFunction<String> format = precise
                ? value -> String.format(Locale.US, "%.1f", value)
                : value -> String.valueOf((int) Math.floor(value));

        return MessageUtils.colorize(registry.getLanguageService().message("Gui.AreaConfigLocation")
                .replace("%world%", location.getWorld() != null ? location.getWorld().getName() : "?")
                .replace("%x%", format.apply(location.getX()))
                .replace("%y%", format.apply(location.getY()))
                .replace("%z%", format.apply(location.getZ())));
    }

    @Override
    protected void clearFields(UUID playerUuid) {
        drafts.remove(playerUuid);
    }
}
