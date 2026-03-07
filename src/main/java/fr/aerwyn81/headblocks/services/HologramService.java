package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.InternalHologram;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
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

    private HashMap<UUID, InternalHologram> foundHolograms;
    private HashMap<UUID, InternalHologram> notFoundHolograms;
    private HashMap<UUID, InternalHologram> holograms;
    private boolean enable;
    private EnumTypeHologram enumTypeHologram;
    private IHologramPool<Hologram> hologramPool;

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
        foundHolograms = new HashMap<>();
        notFoundHolograms = new HashMap<>();
        holograms = new HashMap<>();

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

            Hunt primaryHunt = huntService.getHighestPriorityHuntForHead(loc.getUuid());
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
                foundHolograms.put(holoFound.getUuid(), holoFound);
            }

            if (huntConfig.isHologramsNotFoundEnabled()) {
                var holoNotFound = internalCreateHologram(location, huntConfig.getHologramsNotFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
                notFoundHolograms.put(holoNotFound.getUuid(), holoNotFound);
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var holo = internalCreateHologram(location, Collections.emptyList());
            holograms.put(holo.getUuid(), holo);
        }
    }

    public void ensureHologramsCreated(Location location, HuntConfig huntConfig) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            var existingFound = getHologramByLocation(foundHolograms, location);
            var existingNotFound = getHologramByLocation(notFoundHolograms, location);

            boolean hasAlive = (existingFound != null && existingFound.isAlive())
                    || (existingNotFound != null && existingNotFound.isAlive());

            if (hasAlive) {
                return;
            }

            // Clean up dead entries before recreating
            if (existingFound != null) {
                foundHolograms.remove(existingFound.getUuid());
            }
            if (existingNotFound != null) {
                notFoundHolograms.remove(existingNotFound.getUuid());
            }
        } else if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var existing = getHologramByLocation(holograms, location);
            if (existing != null && existing.isAlive()) {
                return;
            }

            if (existing != null) {
                holograms.remove(existing.getUuid());
            }
        }

        createHolograms(location, huntConfig);
    }

    private InternalHologram internalCreateHologram(Location location, List<String> lines) {
        var allUUIDs = new ArrayList<>(foundHolograms.keySet());
        allUUIDs.addAll(notFoundHolograms.keySet());
        allUUIDs.addAll(holograms.keySet());

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

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null) {
            if (!holoFound.isHologramVisible(player) && !configService.isHideFoundHeads()) {
                holoFound.show(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
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

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null) {
            if (holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
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
            var holo = getHologramByLocation(holograms, headLocation.getLocation());
            if (holo != null && holo.isHologramVisible(player)) {
                holo.hide(player);
            }
            return;
        }

        if (configService.hologramsFoundEnabled()) {
            var holoFound = getHologramByLocation(foundHolograms, headLocation.getLocation());
            if (holoFound != null && holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        if (configService.hologramsNotFoundEnabled()) {
            var holoNotFound = getHologramByLocation(notFoundHolograms, headLocation.getLocation());
            if (holoNotFound != null && holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public void removeHolograms(Location location) {
        if (location == null) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            var foundHolo = getHologramByLocation(foundHolograms, location);
            var notFoundHolo = getHologramByLocation(notFoundHolograms, location);

            if (foundHolo != null) {
                foundHolograms.remove(foundHolo.getUuid());
                foundHolo.deleteHologram();
            }

            if (notFoundHolo != null) {
                notFoundHolograms.remove(notFoundHolo.getUuid());
                notFoundHolo.deleteHologram();
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var uniqueHolo = getHologramByLocation(holograms, location);

            if (uniqueHolo != null) {
                holograms.remove(uniqueHolo.getUuid());
                uniqueHolo.deleteHologram();
            }
        }
    }

    private InternalHologram getHologramByLocation(HashMap<UUID, InternalHologram> list, Location location) {
        var holo = list.entrySet().stream()
                .filter(entry -> LocationUtils.areEquals(entry.getValue().getLocation(), location))
                .findFirst()
                .orElse(null);

        if (holo != null) {
            return holo.getValue();
        }

        return null;
    }

    public void unload() {
        if (!enable) {
            return;
        }

        foundHolograms.forEach((uuid, hologram) -> hologram.deleteHologram());
        notFoundHolograms.forEach((uuid, hologram) -> hologram.deleteHologram());
        holograms.forEach((uuid, hologram) -> hologram.deleteHologram());

        foundHolograms.clear();
        notFoundHolograms.clear();
        holograms.clear();

        if (hologramPool != null) {
            hologramPool.destroy();
        }
    }

    public void refresh(Player player, Location location) {
        if (location == null || enumTypeHologram == EnumTypeHologram.DEFAULT) {
            return;
        }

        var uniqueHolo = getHologramByLocation(holograms, location);
        if (uniqueHolo != null) {
            uniqueHolo.refresh(player);
        }
    }

    public IHologramPool<Hologram> getHologramPool() {
        return hologramPool;
    }

}
