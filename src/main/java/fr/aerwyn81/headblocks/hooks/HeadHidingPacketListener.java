package fr.aerwyn81.headblocks.hooks;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
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

    public void onPlayerJoin(Player player) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        StorageService.getHeadsPlayer(player.getUniqueId()).whenComplete(foundHeads -> {
            if (foundHeads != null) {
                playerFoundHeadsCache.put(player.getUniqueId(), foundHeads);

                // Use entity-aware scheduling for player operations
                HeadBlocks.getInstance().getFoliaLib().getScheduler().runLater(task -> {
                    HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task2 -> {
                        for (var headUuid : foundHeads) {
                            var headLocation = HeadService.getHeadByUUID(headUuid);
                            if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
                                // Block change operations need location-aware scheduling
                                var loc = headLocation.getLocation();
                                HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtLocation(loc, task3 -> {
                                    player.sendBlockChange(loc, org.bukkit.Material.STRUCTURE_VOID.createBlockData());
                                });
                            }
                        }
                    });
                }, 20L);
            }
        });
    }


    public void invalidatePlayerCache(UUID playerUuid) {
        playerFoundHeadsCache.remove(playerUuid);
    }

    public void addFoundHead(Player player, UUID headUuid) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        var foundHeads = playerFoundHeadsCache.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        foundHeads.add(headUuid);

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
            // Use entity + location-aware scheduling
            var loc = headLocation.getLocation();
            HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtLocation(loc, task2 -> {
                    player.sendBlockChange(loc, org.bukkit.Material.STRUCTURE_VOID.createBlockData());
                });
            });
        }
    }

    public void removeFoundHead(Player player, UUID headUuid) {
        var foundHeads = playerFoundHeadsCache.get(player.getUniqueId());
        if (foundHeads != null) {
            foundHeads.remove(headUuid);
        }

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
            var location = headLocation.getLocation();

            // Use entity + location-aware scheduling
            HeadBlocks.getInstance().getFoliaLib().getScheduler().runLater(task -> {
                HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task2 -> {
                    HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtLocation(location, task3 -> {
                        player.sendBlockChange(location, location.getBlock().getBlockData());
                        var world = location.getWorld();
                        if (world != null) {
                            var blockState = location.getBlock().getState();
                            blockState.update(true, false);
                        }
                    });
                });
            }, 1L);
        }
    }

    public void showAllPreviousHeads(Player player) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        var previouslyHiddenHeads = playerFoundHeadsCache.get(player.getUniqueId());

        playerFoundHeadsCache.remove(player.getUniqueId());

        if (previouslyHiddenHeads != null && !previouslyHiddenHeads.isEmpty()) {
            // Use entity + location-aware scheduling
            HeadBlocks.getInstance().getFoliaLib().getScheduler().runLater(task -> {
                HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task2 -> {
                    for (var headUuid : previouslyHiddenHeads) {
                        var headLocation = HeadService.getHeadByUUID(headUuid);
                        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
                            var location = headLocation.getLocation();
                            HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtLocation(location, task3 -> {
                                player.sendBlockChange(location, location.getBlock().getBlockData());
                                var world = location.getWorld();
                                if (world != null) {
                                    var blockState = location.getBlock().getState();
                                    blockState.update(true, false);
                                }
                            });
                        }
                    }
                });
            }, 1L);
        }
    }

    public void clearCache() {
        playerFoundHeadsCache.clear();
    }
}
