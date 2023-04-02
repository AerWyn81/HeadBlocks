package fr.aerwyn81.headblocks.services;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBTrack;
import fr.aerwyn81.headblocks.events.OnPlayerPlaceBlockEvent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuiService {

    private static HashMap<UUID, ItemStack> headItemCache;
    private static HashMap<UUID, ChestGui> openInventories;

    public static void initialize() {
        headItemCache = new HashMap<>();
        openInventories = new HashMap<>();
    }

    public static void clearCache() {
        if (headItemCache != null) {
            headItemCache.clear();
        }
    }

    public static void openOptionsGui(Player p) {
        HBMenu optionsMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOptions"), false, 2);

        //Ordering, Removing, Per-head actions, One-time global head click

        optionsMenu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.HOPPER)
                .setName(LanguageService.getMessage("Gui.OrderName"))
                .setLore(LanguageService.getMessages("Gui.OrderLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> openOrderGui((Player) e.getWhoClicked())));

        optionsMenu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(LanguageService.getMessage("Gui.ClickCounterName"))
                .setLore(LanguageService.getMessages("Gui.ClickCounterLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> openClickCounterGui((Player) e.getWhoClicked())));

        //optionsMenu.setItem(0, 14,  new ItemGUI(new ItemBuilder(Material.DIAMOND)
        //        .setName("Rewards")
        //        .setLore("Lore")
        //        .toItemStack(), true)
        //        .addOnClickEvent(event -> {}));

        //int[] borders = { 0,  1,  2,  3,  4,  5,  6,  7,  8, 9,  10, 11, 14, 15, 16, 17 };
        //IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
        //        index -> optionsMenu.setItem(0, borders[index], new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack()))
        //);

        p.openInventory(optionsMenu.getInventory());
    }

    public static void openOrderGui(Player p) {
        //HBMenu orderMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOrder"), true, 5);
//
        //List<HeadLocation> headLocations = HeadService.getHeadLocations()
        //        .stream()
        //        .sorted((Comparator.comparingInt(HeadLocation::getOrderIndex)))
        //        .collect(Collectors.toList());
//
        //if (headLocations.size() == 0) {
        //    orderMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        //            .setName(LanguageService.getMessage("Gui.NoHeads"))
        //            .toItemStack(), true));
        //} else {
        //    for (int i = 0; i < headLocations.size(); i++) {
        //        HeadLocation headLocation = headLocations.get(i);
//
        //        var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
        //                .setName(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.OrderItemName")
        //                                .replaceAll("%headName%", headLocation.getDisplayedName()), headLocation.getLocation()))
        //                .setLore(LanguageService.getMessages("Gui.OrderItemLore").stream().map(s ->
        //                                s.replaceAll("%position%", headLocation.getDisplayedOrderIndex()))
        //                        .collect(Collectors.toList())).toItemStack(), true)
        //                .addOnClickEvent(event -> {
        //                    if (event.getClick() == ClickType.LEFT) {
        //                        if (headLocation.getOrderIndex() != -1) {
        //                            headLocation.setOrderIndex(headLocation.getOrderIndex() - 1);
        //                            HeadService.saveHeadInConfig(headLocation);
        //                        }
        //                    } else if (event.getClick() == ClickType.RIGHT) {
        //                        if (headLocation.getOrderIndex() != headLocations.size() + 1) {
        //                            headLocation.setOrderIndex(headLocation.getOrderIndex() + 1);
        //                            HeadService.saveHeadInConfig(headLocation);
        //                        }
        //                    }
//
        //                    openOrderGui((Player) event.getWhoClicked());
        //                });
//
        //        orderMenu.addItem(i, orderItemGui);
        //    }
        //}
//
        //p.openInventory(orderMenu.getInventory());
    }

    public static void openClickCounterGui(Player p) {
//        HBMenu clickCounterMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleClickCounter"), true, 5);
//
//        List<HeadLocation> headLocations = HeadService.getHeadLocations()
//                .stream()
//                .sorted(((o1, o2) -> o2.getOrderIndex() - o1.getOrderIndex()))
//                .collect(Collectors.toList());
//
//        if (headLocations.size() == 0) {
//            clickCounterMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
//                    .setName(LanguageService.getMessage("Gui.NoHeads"))
//                    .toItemStack(), true));
//        } else {
//            for (int i = 0; i < headLocations.size(); i++) {
//                HeadLocation headLocation = headLocations.get(i);
//
//                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
//                        .setName(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.CounterClickItemName")
//                                .replaceAll("%headName%", headLocation.getDisplayedName()), headLocation.getLocation()))
//                        .setLore(LanguageService.getMessages("Gui.CounterClickItemLore").stream().map(s ->
//                                        s.replaceAll("%count%", headLocation.getDisplayedHitCount()))
//                                .collect(Collectors.toList())).toItemStack(), true)
//                        .addOnClickEvent(event -> {
//                            if (event.getClick() == ClickType.LEFT) {
//                                if (headLocation.getHitCount() != -1) {
//                                    headLocation.setHitCount(headLocation.getHitCount() - 1);
//                                    HeadService.saveHeadInConfig(headLocation);
//                                }
//                            } else if (event.getClick() == ClickType.RIGHT) {
//                                headLocation.setHitCount(headLocation.getHitCount() + 1);
//                                HeadService.saveHeadInConfig(headLocation);
//                            }
//
//                            openClickCounterGui((Player) event.getWhoClicked());
//                        });
//
//                clickCounterMenu.addItem(i, orderItemGui);
//            }
//        }
//
//        p.openInventory(clickCounterMenu.getInventory());
    }

    public static void showTracksGui(Player player,
                           Consumer<InventoryCloseEvent> eventOnClose,
                           BiConsumer<InventoryClickEvent, HBTrack> eventOnClick,
                           Consumer<InventoryClickEvent> eventOnPaginationBack,
                           Function<HBTrack, ArrayList<String>> lore,
                           boolean withCreateTrack,
                           Location headLocation,
                           String headTexture) {
        var gui = new ChestGui(6, LanguageService.getMessage("Gui.TracksTitle"));
        gui.setOnClose(eventOnClose);

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        var pages = new PaginatedPane(0, 0, 9, 5);
        pages.populateWithGuiItems(TrackService.getTracks().values().stream()
                .map(track -> new GuiItem(new ItemBuilder(track.getIcon())
                        .setName(MessageUtils.colorize("&e" + track.getId()) + ". " + track.getDisplayName())
                        .setLore(lore.apply(track))
                        .toItemStack(),
                        click -> eventOnClick.accept(click, track)))
                .collect(Collectors.toList()));

        gui.addPane(pages);

        var navigation = setPaginationLayout(gui, pages, false, eventOnPaginationBack);

        if (withCreateTrack) {
            navigation.addItem(new GuiItem(new ItemBuilder(Material.NETHER_STAR)
                    .setName(LanguageService.getMessage("Gui.CreateNewTrackName"))
                    .setLore(LanguageService.getMessages("Gui.CreateNewTrackLore"))
                    .toItemStack(), event -> {
                gui.setOnClose(null);
                closeInventory(player);

                var conversation = ConversationService.askForTrackName(player, headLocation, headTexture);
                conversation.addConversationAbandonedListener(e -> {
                    if (!e.gracefulExit()) {
                        OnPlayerPlaceBlockEvent.cancelChoice(player, headLocation);
                    }
                });
            }), 4, 0);
        }

        gui.addPane(navigation);
        gui.show(player);

        openInventories.put(player.getUniqueId(), gui);
    }

    public static void showTracksGuiWithBack(Player player, HBTrack chosenTrack, Function<HBTrack, ArrayList<String>> lore) {
        if (chosenTrack == null) {
            GuiService.showTracksGui(player,
                    inventoryCloseEvent -> { },
                    (inventoryClickEvent, hbTrack) -> GuiService.showContentTracksGui(player, hbTrack, e -> showTracksGuiWithBack(player, null, lore)),
                    inventoryClickEvent -> GuiService.closeInventory(inventoryClickEvent.getWhoClicked()),
                    lore, false,null, null);

            return;
        }

        GuiService.showContentTracksGui(player, chosenTrack, e -> showTracksGuiWithBack(player, null, lore));
    }

    public static void showContentTracksGui(Player player, HBTrack track, Consumer<InventoryClickEvent> eventOnPaginationBack) {
        var heads = new ArrayList<>(track.getHeadManager().getHeadLocations());

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            heads.removeIf(h -> !h.isCharged());
        }

        var gui = new ChestGui(6, LanguageService.getMessage("Gui.TrackContentTitle")
                .replaceAll("%headCount%", String.valueOf(heads.size())));

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        var pages = new PaginatedPane(0, 0, 9, 5);

        pages.populateWithGuiItems(heads.stream()
                .map(head -> {
                    StringBuilder lore = new StringBuilder();

                    if (head.getDescription().size() > 0) {
                        lore.append(String.join("\n", head.getDisplayedDescription())).append("\n");
                    }

                    if (PlayerUtils.hasPermission(player, "headblocks.admin")) {
                        lore.append("\n");
                        lore.append(LanguageService.getMessage("Gui.Coordinates")).append("\n");
                        if (head.getLocation() != null) {
                            lore.append(MessageUtils.colorize("&7 X: &a" + head.getLocation().getBlockX())).append("\n")
                                    .append(MessageUtils.colorize("&7 Z: &a" + head.getLocation().getBlockZ())).append("\n")
                                    .append(MessageUtils.colorize("&7 Y: &a" + head.getLocation().getBlockY())).append("\n")
                                    .append(MessageUtils.colorize("&7 " + LanguageService.getMessage("Gui.World") + " &a" + MessageUtils.parseWorld(head.getLocation()))).append("\n");
                        } else {
                            lore.append(LanguageService.getMessage("Gui.ErrorLoadingLocation")).append("\n");
                        }

                        lore.append("\n");

                        if (head.isCharged()) {
                            lore.append(LanguageService.getMessage("Gui.HeadLoaded")).append("\n");
                        } else {
                            lore.append(LanguageService.getMessage("Gui.HeadNotLoaded")).append("\n");
                        }

                        lore.append("\n")
                            .append(LanguageService.getMessage("Gui.ClickTeleport"));
                    }

                    var itemStackGui = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    if (head.isCharged()) {
                        itemStackGui = HeadUtils.applyTextureToItemStack(new ItemStack(Material.PLAYER_HEAD), head.getTexture());
                    }

                    return new GuiItem(new ItemBuilder(itemStackGui)
                            .setName(head.getDisplayedName())
                            .setLore(lore.toString().split("\n"))
                            .toItemStack(),
                            click -> {
                                if (head.getLocation() == null) {
                                    return;
                                }

                                var teleportLocation = head.getLocation().clone();
                                teleportLocation.add(0.5D, 0.5D, 0.5D);
                                teleportLocation.setYaw(0F);
                                teleportLocation.setPitch(90F);
                                player.teleport(teleportLocation);
                            });
                })
                .collect(Collectors.toList()));

        gui.addPane(pages);

        var navigation = setPaginationLayout(gui, pages, true, eventOnPaginationBack);

        gui.addPane(navigation);
        gui.show(player);
    }

    private static StaticPane setPaginationLayout(ChestGui gui, PaginatedPane pages, boolean isButtonBackCancel, Consumer<InventoryClickEvent> eventOnPaginationBack) {
        var background = new OutlinePane(0, 5, 9, 1);
        background.addItem(new GuiItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("").toItemStack()));
        background.setRepeat(true);
        background.setPriority(Pane.Priority.LOWEST);

        gui.addPane(background);

        var navigation = new StaticPane(0, 5, 9, 1);
        navigation.addItem(new GuiItem(new ItemBuilder(Material.ARROW)
                .setName(LanguageService.getMessage("Gui.PreviousPage")).toItemStack(), event ->
        {
            if (pages.getPage() > 0) {
                pages.setPage(pages.getPage() - 1);
                gui.update();
            }
        }), 3, 0);

        navigation.addItem(new GuiItem(new ItemBuilder(Material.ARROW)
                .setName(LanguageService.getMessage("Gui.NextPage")).toItemStack(), event ->
        {
            if (pages.getPage() < pages.getPages() - 1) {
                pages.setPage(pages.getPage() + 1);
                gui.update();
            }
        }), 5, 0);

        navigation.addItem(new GuiItem(new ItemBuilder(Material.SPRUCE_DOOR)
                .setName(isButtonBackCancel ? LanguageService.getMessage("Gui.Cancel") : LanguageService.getMessage("Gui.Close")).toItemStack(), eventOnPaginationBack
        ), 8, 0);

        return navigation;
    }

    public static void closeInventory(HumanEntity entity) {
        entity.closeInventory();
        openInventories.remove(entity.getUniqueId());
    }

    public static void closeAllInventories() {
        openInventories.forEach((uuid, chestGui) -> {
            var onlinePlayer = Bukkit.getOnlinePlayers().stream().filter(p -> p.getUniqueId() == uuid).findFirst();
            onlinePlayer.ifPresent(player -> {
                player.sendMessage(LanguageService.getMessage("Messages.GuiClosePlayerOnReload"));
                player.closeInventory();
            });
        });

        openInventories.clear();
    }

    public static Optional<ChestGui> getOpenedInventory(Player player) {
        return Optional.ofNullable(openInventories.get(player.getUniqueId()));
    }
}
