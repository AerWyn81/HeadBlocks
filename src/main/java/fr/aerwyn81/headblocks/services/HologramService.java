package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.InternalHologram;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.HoloEasy;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.pool.IHologramPool;

import java.util.*;
import java.util.stream.Collectors;

public class HologramService {
    private final ConfigService configService;
    private final HuntService huntService;
    private final PluginProvider pluginProvider;
    private final HoloEasy holoEasyLib;

    private HeadService headService;
    private ServiceRegistry serviceRegistry;

    private HologramMap foundHolograms;
    private HologramMap notFoundHolograms;
    private HologramMap holograms;
    private boolean enable;
    private EnumTypeHologram enumTypeHologram;
    private IHologramPool<Hologram> hologramPool;

    private static class HologramMap {
        private final HashMap<UUID, InternalHologram> byUuid = new HashMap<>();
        private final HashMap<String, UUID> byLocation = new HashMap<>();

        private static String locationKey(Location loc) {
            return (loc.getWorld() == null
                    ? ""
                    : loc.getWorld().getName() + ":")
                    + loc.getBlockX() + ":"
                    + loc.getBlockY() + ":"
                    + loc.getBlockZ();
        }

        void put(InternalHologram holo, Location location) {
            byUuid.put(holo.getUuid(), holo);
            byLocation.put(locationKey(location), holo.getUuid());
        }

        InternalHologram getByLocation(Location location) {
            if (location.getWorld() == null) {
                return null;
            }
            UUID uuid = byLocation.get(locationKey(location));
            if (uuid != null) {
                return byUuid.get(uuid);
            }
            return null;
        }

        void remove(InternalHologram holo, Location location) {
            byUuid.remove(holo.getUuid());
            byLocation.remove(locationKey(location));
        }

        void deleteAll() {
            byUuid.values().forEach(InternalHologram::deleteHologram);
            byUuid.clear();
            byLocation.clear();
        }

        Collection<UUID> uuids() {
            return byUuid.keySet();
        }
    }

    // --- Constructor ---

    public HologramService(ConfigService configService, HuntService huntService,
                           PluginProvider pluginProvider, HoloEasy holoEasyLib) {
        this.configService = configService;
        this.huntService = huntService;
        this.pluginProvider = pluginProvider;
        this.holoEasyLib = holoEasyLib;
        this.enable = true;
    }

    public void setHeadService(HeadService headService) {
        this.headService = headService;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    // --- Instance methods ---

    public void load() {
        foundHolograms = new HologramMap();
        notFoundHolograms = new HologramMap();
        holograms = new HologramMap();

        enable = configService.hologramsEnabled();
        if (!enable) {
            return;
        }

        enumTypeHologram = getHologramTypeFromConfig();
        if (enumTypeHologram == null) {
            if (pluginProvider.isPacketEventsActive()) {
                enumTypeHologram = EnumTypeHologram.ADVANCED;
                LogUtil.warning("{0} plugin support removed. PacketEvents detected, switching to internal advanced hologram type.", configService.hologramPlugin());
            } else {
                enumTypeHologram = EnumTypeHologram.DEFAULT;
                LogUtil.warning("{0} plugin support removed. Switching to default hologram type. For placeholder support, install PacketEvents plugin and set hologram type to ADVANCED in config.", configService.hologramPlugin());
            }
        }

        if (holoEasyLib != null) {
            hologramPool = holoEasyLib.startPool(configService.hologramParticlePlayerViewDistance(), false, true);
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED && !pluginProvider.isPacketEventsActive()) {
            LogUtil.error("Cannot use ADVANCED hologram type without PacketEvents plugin. Please install PacketEvents plugin or change hologram type to DEFAULT in config.yml");
            enable = false;
            return;
        }

        for (HeadLocation loc : headService.getHeadLocations()) {
            if (!loc.isCharged()) {
                continue;
            }

            var headLoc = loc.getLocation();
            if (headLoc.getWorld() == null) {
                continue;
            }

            // Skip heads in unloaded chunks — they will be created lazily by GlobalTask
            if (!headLoc.getWorld().isChunkLoaded(headLoc.getBlockX() >> 4, headLoc.getBlockZ() >> 4)) {
                continue;
            }

            HBHunt primaryHunt = huntService.getHighestPriorityHuntForHead(loc.getUuid());
            HuntConfig huntConfig = primaryHunt != null ? primaryHunt.getConfig() : new HuntConfig(configService);
            createHolograms(headLoc, huntConfig);
        }
    }

    public EnumTypeHologram getHologramTypeFromConfig() {
        return EnumTypeHologram.getEnumFromText(configService.hologramPlugin());
    }

    public void createHolograms(Location location) {
        createHolograms(location, new HuntConfig(configService));
    }

    public void createHolograms(Location location, HuntConfig huntConfig) {
        if (!enable) {
            var suffix = "";

            if (enumTypeHologram != EnumTypeHologram.DEFAULT && enumTypeHologram != EnumTypeHologram.ADVANCED) {
                suffix = "Is " + EnumTypeHologram.getPluginName(enumTypeHologram) + " plugin enabled?";
            }

            LogUtil.error("Cannot create an hologram above the head at {0}", location.toString(), suffix);
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            if (huntConfig.isHologramsFoundEnabled()) {
                var holoFound = internalCreateHologram(location, huntConfig.getHologramsFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
                foundHolograms.put(holoFound, location);
            }

            if (huntConfig.isHologramsNotFoundEnabled()) {
                var holoNotFound = internalCreateHologram(location, huntConfig.getHologramsNotFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
                notFoundHolograms.put(holoNotFound, location);
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var holo = internalCreateHologram(location, Collections.emptyList());
            holograms.put(holo, location);
        }
    }

    public void ensureHologramsCreated(Location location, HuntConfig huntConfig) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            var existingFound = foundHolograms.getByLocation(location);
            var existingNotFound = notFoundHolograms.getByLocation(location);

            boolean hasAlive = (existingFound != null && existingFound.isAlive())
                    || (existingNotFound != null && existingNotFound.isAlive());

            if (hasAlive) {
                return;
            }

            // Clean up dead entries before recreating
            if (existingFound != null) {
                foundHolograms.remove(existingFound, location);
            }
            if (existingNotFound != null) {
                notFoundHolograms.remove(existingNotFound, location);
            }
        } else if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var existing = holograms.getByLocation(location);
            if (existing != null && existing.isAlive()) {
                return;
            }

            if (existing != null) {
                holograms.remove(existing, location);
            }
        }

        createHolograms(location, huntConfig);
    }

    private InternalHologram internalCreateHologram(Location location, List<String> lines) {
        var allUUIDs = new ArrayList<>(foundHolograms.uuids());
        allUUIDs.addAll(notFoundHolograms.uuids());
        allUUIDs.addAll(holograms.uuids());

        var uuid = InternalUtils.generateNewUUID(allUUIDs);
        var internalHologram = new InternalHologram(enumTypeHologram, serviceRegistry);
        internalHologram.createHologram(uuid, location, lines, configService.hologramsHeightAboveHead());

        return internalHologram;
    }

    public void showFoundTo(Player player, Location location, HuntConfig huntConfig) {
        if (!enable) {
            return;
        }

        boolean foundEnabled = huntConfig != null ? huntConfig.isHologramsFoundEnabled() : configService.hologramsFoundEnabled();
        if (enumTypeHologram == EnumTypeHologram.ADVANCED || !foundEnabled) {
            return;
        }

        var holoFound = foundHolograms.getByLocation(location);
        if (holoFound != null) {
            if (!holoFound.isHologramVisible(player) && !configService.isHideFoundHeads()) {
                holoFound.show(player);
            }
        }

        var holoNotFound = notFoundHolograms.getByLocation(location);
        if (holoNotFound != null) {
            if (holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public void showNotFoundTo(Player player, Location location, HuntConfig huntConfig) {
        if (!enable) {
            return;
        }

        boolean notFoundEnabled = huntConfig != null ? huntConfig.isHologramsNotFoundEnabled() : configService.hologramsNotFoundEnabled();
        if (enumTypeHologram == EnumTypeHologram.ADVANCED || !notFoundEnabled) {
            return;
        }

        var holoFound = foundHolograms.getByLocation(location);
        if (holoFound != null) {
            if (holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        var holoNotFound = notFoundHolograms.getByLocation(location);
        if (holoNotFound != null) {
            if (!holoNotFound.isHologramVisible(player)) {
                holoNotFound.show(player);
            }
        }
    }

    public void hideHolograms(HeadLocation headLocation, Player player) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var holo = holograms.getByLocation(headLocation.getLocation());
            if (holo != null && holo.isHologramVisible(player)) {
                holo.hide(player);
            }
            return;
        }

        if (configService.hologramsFoundEnabled()) {
            var holoFound = foundHolograms.getByLocation(headLocation.getLocation());
            if (holoFound != null && holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        if (configService.hologramsNotFoundEnabled()) {
            var holoNotFound = notFoundHolograms.getByLocation(headLocation.getLocation());
            if (holoNotFound != null && holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public void removeHolograms(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            var foundHolo = foundHolograms.getByLocation(location);
            if (foundHolo != null) {
                foundHolograms.remove(foundHolo, location);
                foundHolo.deleteHologram();
            }

            var notFoundHolo = notFoundHolograms.getByLocation(location);
            if (notFoundHolo != null) {
                notFoundHolograms.remove(notFoundHolo, location);
                notFoundHolo.deleteHologram();
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var uniqueHolo = holograms.getByLocation(location);
            if (uniqueHolo != null) {
                holograms.remove(uniqueHolo, location);
                uniqueHolo.deleteHologram();
            }
        }
    }

    public void unload() {
        if (!enable) {
            return;
        }

        foundHolograms.deleteAll();
        notFoundHolograms.deleteAll();
        holograms.deleteAll();

        if (hologramPool != null) {
            //noinspection UnstableApiUsage
            hologramPool.destroy();
        }
    }

    public void refresh(Player player, Location location) {
        if (location == null || enumTypeHologram == EnumTypeHologram.DEFAULT) {
            return;
        }

        var uniqueHolo = holograms.getByLocation(location);
        if (uniqueHolo != null) {
            uniqueHolo.refresh(player);
        }
    }

    public IHologramPool<Hologram> getHologramPool() {
        return hologramPool;
    }

}
