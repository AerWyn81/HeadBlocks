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

                Bukkit.getScheduler().runTaskLater(HeadBlocks.getInstance(), () -> {
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
    }

    public void addFoundHead(Player player, UUID headUuid) {
        if (!ConfigService.hideFoundHeads()) {
            return;
        }

        var foundHeads = playerFoundHeadsCache.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        foundHeads.add(headUuid);

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
            player.sendBlockChange(headLocation.getLocation(),
                    org.bukkit.Material.STRUCTURE_VOID.createBlockData());
        }
    }

    public void removeFoundHead(Player player, UUID headUuid) {
        var foundHeads = playerFoundHeadsCache.get(player.getUniqueId());
        if (foundHeads != null) {
            foundHeads.remove(headUuid);
        }

        var headLocation = HeadService.getHeadByUUID(headUuid);
        if (headLocation != null && player.getWorld().equals(headLocation.getLocation().getWorld())) {
            var block = headLocation.getLocation().getBlock();
            player.sendBlockChange(headLocation.getLocation(), block.getBlockData());
        }
    }

    public void clearCache() {
        playerFoundHeadsCache.clear();
    }
}
