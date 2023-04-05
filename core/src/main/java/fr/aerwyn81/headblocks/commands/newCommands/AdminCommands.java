package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HBTrack;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.ExportSQLHelper;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission("headblocks.admin")
public class AdminCommands {

    @Subcommand({"exportDatabase"})
    @CommandPermission("headblocks.admin.exportDatabase")
    @Description("Export command")
    @Usage("<SQLite|MySQL>")
    public void export(BukkitCommandActor actor, @Named("database_type") EnumTypeDatabase typeDatabase) {
        actor.reply(MessageUtils.colorize(LanguageService.getMessage("Messages.ExportInProgress")));

        ExportSQLHelper.generateFile(typeDatabase)
                .exceptionally((ex) -> {
                    actor.reply(LanguageService.getMessage("Messages.ExportError") + ex.getMessage());
                    return null;
                })
                .thenAcceptAsync(fileName -> actor.reply(LanguageService.getMessage("Messages.ExportSuccess")
                        .replaceAll("%fileName%", fileName)));
    }

    @Subcommand("give")
    @CommandPermission("headblocks.admin.give")
    @Description("Give command")
    @Usage("(player) <number>")
    public void give(BukkitCommandActor actor, @Default Player player, @Named("number") @Optional Integer part) {
        var hbHeads = new ArrayList<>(HeadService.getHeads());

        var headNotLoaded = hbHeads.stream()
                .filter(HBHeadHDB.class::isInstance)
                .map(h -> (HBHeadHDB) h)
                .filter(HBHeadHDB::isLoaded)
                .collect(Collectors.toList());

        if (headNotLoaded.size() > 0) {
            actor.reply(LanguageService.getMessage("Messages.HeadNotYetLoaded")
                    .replaceAll("%ids%", headNotLoaded.stream().map(HBHeadHDB::getId).collect(Collectors.joining(", "))));

            // At least one HeadDatabase head not loaded, removing all others to give
            hbHeads.removeIf(hbHead -> hbHead instanceof HBHeadHDB);
        }

        var headsToItemstack = hbHeads.stream().map(HBHead::getItemStack).toArray(ItemStack[]::new);

        var freeSlots = PlayerUtils.getFreeSlots(player, headsToItemstack);
        if (freeSlots == 0) {
            actor.reply(LanguageService.getMessage("Messages.InventoryFull"));
            return;
        }

        if (freeSlots >= hbHeads.size()) {
            player.getInventory().addItem(headsToItemstack);
            actor.reply(LanguageService.getMessage("Messages.HeadGiven")
                    .replaceAll("%headNumber%", String.valueOf(hbHeads.size())));

            return;
        }

        if (part == -1) {
            actor.reply(LanguageService.getMessage("Messages.GiveTooManyHeadInventory"));
            return;
        }

        if (part <= 0)
            part = 1;
        else if (part > hbHeads.size())
            part = hbHeads.size() - 1;

        var headSelected  = hbHeads.get(part);
        if (headSelected != null) {
            player.getInventory().addItem(headSelected.getItemStack());
            actor.reply(LanguageService.getMessage("Messages.HeadGiven")
                    .replaceAll("%headNumber%", "1"));
        }
    }

    @Subcommand({"tracks"})
    @CommandPermission("headblocks.admin.list")
    @Description("Track list command")
    @Usage("(track_name)")
    public void tracks(BukkitCommandActor actor, @Optional HBTrack track) {
        var player = actor.requirePlayer();

        GuiService.showTracksGuiWithBack(player, track, t -> {
            StringBuilder lore = new StringBuilder();

            var message = LanguageService.getMessage("Gui.TrackItemHeadCount")
                    .replaceAll("%headCount%", String.valueOf(t.getHeadCount()));
            if (message.trim().length() > 0) {
                lore.append(message).append("\n");
            }

            lore.append("\n");

            if (t.getDescription().size() > 0) {
                for (var line : t.getColorizedDescription()) {
                    lore.append(line).append("\n");
                }

                lore.append("\n");
            }

            lore.append(LanguageService.getMessage("Gui.ClickShowContent"));
            return new ArrayList<>(List.of(lore.toString().split("\n")));
        });
    }

    @DefaultFor("hb move")
    @CommandPermission("headblocks.admin.move")
    @Description("Move command")
    @Usage("(track) (--cancel|--confirm)")
    @AutoComplete("@internal *")
    public void move(BukkitCommandActor actor, @Optional HBTrack track) {
        var player = actor.requirePlayer();

        if (HeadService.getHeadMoves().containsKey(player.getUniqueId())) {
            actor.reply(LanguageService.getMessage("Messages.HeadMoveAlready"));
            return;
        }

        Location targetLoc = player.getTargetBlock(null, 30).getLocation();

        java.util.Optional<HeadLocation> optHeadLocation = TrackService.getHeadAt(targetLoc);
        if (optHeadLocation.isEmpty()) {
            actor.reply(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return;
        }

        var headLocation = optHeadLocation.get();

        if (headLocation.getHeadManager().getTrack() == track) {
            actor.reply(LanguageService.getMessage("Messages.HeadMoveSameTrack")
                    .replaceAll("%track%", track.getDisplayName()));
            return;
        }

        String message = LanguageService.getMessage("Messages.TargetBlockInfo")
                .replaceAll("%uuid%", headLocation.getUuid().toString());

        HeadService.getHeadMoves().put(player.getUniqueId(), new HeadMove(headLocation.getUuid(), headLocation.getLocation(), track == null ? null : track.getId()));

        actor.reply(MessageUtils.parseLocationPlaceholders(message, headLocation.getLocation()));
    }

    @Subcommand({"move --cancel"})
    @CommandPermission("headblocks.admin.move")
    public void moveCancel(BukkitCommandActor actor) {
        var player = actor.requirePlayer();

        if (HeadService.getHeadMoves().remove(player.getUniqueId()) != null) {
            actor.reply(LanguageService.getMessage("Messages.HeadMoveCancel"));
        }
    }

    @Subcommand({"move --confirm"})
    @CommandPermission("headblocks.admin.move")
    public void moveConfirm(BukkitCommandActor actor) {
        var player = actor.requirePlayer();

        var optHeadMove = HeadService.getHeadMoves().entrySet().stream()
                .filter(uuidHeadMoveEntry -> player.getUniqueId() == uuidHeadMoveEntry.getKey()).findFirst();
        if (optHeadMove.isEmpty()) {
            actor.reply(LanguageService.getMessage("Messages.HeadMoveNoPlayer"));
            return;
        }

        var headOldLoc = optHeadMove.get().getValue();

        var targetLoc = player.getTargetBlock(null, 30).getLocation();

        var newHeadBlockLoc = targetLoc.clone().add(0, 1, 0);
        if (LocationUtils.areEquals(newHeadBlockLoc, headOldLoc.getOldLoc())) {
            actor.reply(LanguageService.getMessage("Messages.HeadMoveOtherLoc"));
            return;
        }
        if (isTargetBlockInvalid(targetLoc.getBlock()) || !newHeadBlockLoc.getBlock().isEmpty()) {
            actor.reply(LanguageService.getMessage("Messages.TargetBlockInvalid"));
        }

        java.util.Optional<HeadLocation> optHeadLocation = TrackService.getHeadAt(headOldLoc.getOldLoc());
        if (optHeadLocation.isEmpty()) {
            actor.reply(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return;
        }

        var headLocation = optHeadLocation.get();

        TrackService.changeHeadLocation(headLocation, headOldLoc.getOldLoc().getBlock(), newHeadBlockLoc.getBlock(), headOldLoc.getTrackId());
        HeadService.getHeadMoves().remove(player.getUniqueId());

        actor.reply(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.TargetBlockMoved"), newHeadBlockLoc));
    }

    private boolean isTargetBlockInvalid(Block block) {
        return block.isEmpty() || block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD;
    }

    @Subcommand("reload")
    @CommandPermission("headblocks.admin.reload")
    @Description("Reload command")
    public void reload(BukkitCommandActor actor) {
        var plugin = HeadBlocks.getInstance();
        HeadBlocks.isReloadInProgress = true;

        plugin.reloadConfig();
        ConfigService.load();

        LanguageService.setLanguage(ConfigService.getLanguage());
        LanguageService.pushMessages();

        HologramService.unload();
        StorageService.close();

        StorageService.initialize();
        for (Player player : Collections.synchronizedCollection(Bukkit.getOnlinePlayers())) {
            StorageService.unloadPlayer(player);
            StorageService.loadPlayer(player);
        }

        plugin.getParticlesTask().cancel();

        HologramService.load();
        HeadService.load();
        GuiService.clearCache();

        ConversationService.clearConversations();
        GuiService.closeAllInventories();

        TrackService.load();

        if (plugin.isHeadDatabaseActive()) {
            if (plugin.getHeadDatabaseHook() == null) {
                plugin.setHeadDatabaseHook(new HeadDatabaseHook());
            }

            plugin.getHeadDatabaseHook().loadHeadsHDB();
        }

        plugin.setParticlesTask(new GlobalTask());
        plugin.getParticlesTask().runTaskTimer(plugin, 0, ConfigService.getDelayGlobalTask());

        HeadBlocks.isReloadInProgress = false;

        if (StorageService.hasStorageError()) {
            actor.reply(LanguageService.getMessage("Messages.ReloadWithErrors"));
        }

        actor.reply(LanguageService.getMessage("Messages.ReloadComplete"));
    }


}
