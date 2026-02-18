package fr.aerwyn81.headblocks.hooks;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadHidingPacketListener implements PacketListener {

    private final ConcurrentHashMap<UUID, Set<UUID>> playerFoundHeadsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Set<UUID>>> playerChunkHeadsCache = new ConcurrentHashMap<>();

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        Player player = event.getPlayer();
        //noinspection ConstantValue
        if (player == null) {
            return;
        }

        var foundHeads = playerFoundHeadsCache.get(player.getUniqueId());
        if (foundHeads == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChange(event, player, foundHeads);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            handleMultiBlockChange(event, player, foundHeads);
        } else if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            handleChunkData(event, player);
        }
    }

    private void handleBlockChange(PacketSendEvent event, Player player, Set<UUID> foundHeads) {
        var packet = new WrapperPlayServerBlockChange(event);

        var blockPos = packet.getBlockPosition();
        var loc = new Location(player.getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ());

        var headLocation = HeadService.getHeadAt(loc);
        if (headLocation != null && foundHeads.contains(headLocation.getUuid())) {
            packet.setBlockState(WrappedBlockState.getDefaultState(StateTypes.STRUCTURE_VOID));
        }
    }

    private void handleMultiBlockChange(PacketSendEvent event, Player player, Set<UUID> foundHeads) {
        var packet = new WrapperPlayServerMultiBlockChange(event);

        var blocks = packet.getBlocks();
        for (int i = 0; i < blocks.length; i++) {
            var block = blocks[i];

            var loc = new Location(player.getWorld(), block.getX(), block.getY(), block.getZ());

            var headLocation = HeadService.getHeadAt(loc);
            if (headLocation != null && foundHeads.contains(headLocation.getUuid())) {
                blocks[i] = new WrapperPlayServerMultiBlockChange.EncodedBlock(
                        WrappedBlockState.getDefaultState(StateTypes.STRUCTURE_VOID),
                        block.getX(),
                        block.getY(),
                        block.getZ()
                );
            }
        }
        packet.setBlocks(blocks);
    }

    private void handleChunkData(PacketSendEvent event, Player player) {
        var packet = new WrapperPlayServerChunkData(event);

        var chunkX = packet.getColumn().getX();
        var chunkZ = packet.getColumn().getZ();
        long chunkKey = getChunkKey(chunkX, chunkZ);

        var chunkHeadsMap = playerChunkHeadsCache.get(player.getUniqueId());
        if (chunkHeadsMap == null) {
            return;
        }

        var headsInChunk = chunkHeadsMap.get(chunkKey);
        if (headsInChunk == null || headsInChunk.isEmpty()) {
            return;
        }

        // Schedule a task to send block changes after the chunk is loaded on the client side
        HeadBlocks.getScheduler().runAtEntityLater(player, () -> {
            for (var headUuid : headsInChunk) {
                var headLocation = HeadService.getHeadByUUID(headUuid);
                if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
                    player.sendBlockChange(headLocation.getLocation(),
                            org.bukkit.Material.STRUCTURE_VOID.createBlockData());
                }
            }
        }, 1L);
    }

    private long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public void onPlayerJoin(Player player) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        StorageService.getHeadsPlayer(player.getUniqueId()).whenComplete(foundHeads -> {
            if (foundHeads != null) {
                playerFoundHeadsCache.put(player.getUniqueId(), foundHeads);

                var chunkHeadsMap = new ConcurrentHashMap<Long, Set<UUID>>();
                for (var headUuid : foundHeads) {
                    var headLocation = HeadService.getHeadByUUID(headUuid);
                    if (headLocation != null) {
                        var loc = headLocation.getLocation();
                        int chunkX = loc.getBlockX() >> 4;
                        int chunkZ = loc.getBlockZ() >> 4;
                        long chunkKey = getChunkKey(chunkX, chunkZ);

                        chunkHeadsMap.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(headUuid);
                    }
                }
                playerChunkHeadsCache.put(player.getUniqueId(), chunkHeadsMap);

                HeadBlocks.getScheduler().runAtEntityLater(player, (task) -> {
                    for (var headUuid : foundHeads) {
                        var headLocation = HeadService.getHeadByUUID(headUuid);
                        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
                            player.sendBlockChange(headLocation.getLocation(),
                                    org.bukkit.Material.STRUCTURE_VOID.createBlockData());
                        }
                    }
                }, 20L);
            }
        });
    }


    public void invalidatePlayerCache(UUID playerUuid) {
        playerFoundHeadsCache.remove(playerUuid);
        playerChunkHeadsCache.remove(playerUuid);
    }

    public void addFoundHead(Player player, UUID headUuid) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        var foundHeads = playerFoundHeadsCache.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        foundHeads.add(headUuid);

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null) {
            var loc = headLocation.getLocation();
            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            long chunkKey = getChunkKey(chunkX, chunkZ);

            var chunkHeadsMap = playerChunkHeadsCache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
            chunkHeadsMap.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(headUuid);

            if (player.getWorld().equals(loc.getWorld())) {
                player.sendBlockChange(loc, org.bukkit.Material.STRUCTURE_VOID.createBlockData());
            }
        }
    }

    public void removeFoundHead(Player player, UUID headUuid) {
        var foundHeads = playerFoundHeadsCache.get(player.getUniqueId());
        if (foundHeads != null) {
            foundHeads.remove(headUuid);
        }

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null) {
            var loc = headLocation.getLocation();
            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            long chunkKey = getChunkKey(chunkX, chunkZ);

            var chunkHeadsMap = playerChunkHeadsCache.get(player.getUniqueId());
            if (chunkHeadsMap != null) {
                var headsInChunk = chunkHeadsMap.get(chunkKey);
                if (headsInChunk != null) {
                    headsInChunk.remove(headUuid);
                    if (headsInChunk.isEmpty()) {
                        chunkHeadsMap.remove(chunkKey);
                    }
                }
            }

            if (player.getWorld().equals(loc.getWorld())) {
                HeadBlocks.getScheduler().runAtLocationLater(loc, () -> {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    var world = loc.getWorld();
                    if (world != null) {
                        var blockState = loc.getBlock().getState();
                        blockState.update(true, false);
                    }
                }, 1L);
            }
        }
    }

    public void showAllPreviousHeads(Player player) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        var previouslyHiddenHeads = playerFoundHeadsCache.get(player.getUniqueId());

        playerFoundHeadsCache.remove(player.getUniqueId());
        playerChunkHeadsCache.remove(player.getUniqueId());

        if (previouslyHiddenHeads != null && !previouslyHiddenHeads.isEmpty()) {
            for (var headUuid : previouslyHiddenHeads) {
                var headLocation = HeadService.getHeadByUUID(headUuid);
                if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
                    var location = headLocation.getLocation();
                    HeadBlocks.getScheduler().runAtLocationLater(location, () -> {
                        player.sendBlockChange(location, location.getBlock().getBlockData());
                        var world = location.getWorld();
                        if (world != null) {
                            var blockState = location.getBlock().getState();
                            blockState.update(true, false);
                        }
                    }, 1L);
                }
            }
        }
    }

    public void clearCache() {
        playerFoundHeadsCache.clear();
        playerChunkHeadsCache.clear();
    }
}
