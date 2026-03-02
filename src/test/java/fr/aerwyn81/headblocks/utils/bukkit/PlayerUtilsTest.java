package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerUtilsTest {

    // --- hasPermission ---

    @Test
    void hasPermission_nonPlayer_returnsTrue() {
        CommandSender sender = mock(CommandSender.class);

        assertThat(PlayerUtils.hasPermission(sender, "headblocks.admin")).isTrue();
    }

    @Test
    void hasPermission_playerWithPermission_returnsTrue() {
        Player player = mock(Player.class);
        when(player.hasPermission("headblocks.admin")).thenReturn(true);
        when(player.isOp()).thenReturn(false);

        assertThat(PlayerUtils.hasPermission(player, "headblocks.admin")).isTrue();
    }

    @Test
    void hasPermission_playerNoPermissionButOp_returnsTrue() {
        Player player = mock(Player.class);
        when(player.hasPermission("headblocks.admin")).thenReturn(false);
        when(player.isOp()).thenReturn(true);

        assertThat(PlayerUtils.hasPermission(player, "headblocks.admin")).isTrue();
    }

    @Test
    void hasPermission_playerNoPermissionNotOp_returnsFalse() {
        Player player = mock(Player.class);
        when(player.hasPermission("headblocks.admin")).thenReturn(false);
        when(player.isOp()).thenReturn(false);

        assertThat(PlayerUtils.hasPermission(player, "headblocks.admin")).isFalse();
    }

    // --- getEmptySlots ---

    @Test
    void getEmptySlots_allNull_returnsFullCount() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);

        assertThat(PlayerUtils.getEmptySlots(player)).isEqualTo(36);
    }

    @Test
    void getEmptySlots_allFull_returnsZero() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        ItemStack[] items = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = mock(ItemStack.class);
            when(item.getType()).thenReturn(Material.DIAMOND);
            items[i] = item;
        }
        when(inventory.getStorageContents()).thenReturn(items);

        assertThat(PlayerUtils.getEmptySlots(player)).isEqualTo(0);
    }

    @Test
    void getEmptySlots_mixedNullAndItems_returnsCorrectCount() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        ItemStack[] items = new ItemStack[4];
        items[0] = null;
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.getType()).thenReturn(Material.DIAMOND);
        items[1] = diamond;
        items[2] = null;
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        items[3] = air;
        when(inventory.getStorageContents()).thenReturn(items);

        assertThat(PlayerUtils.getEmptySlots(player)).isEqualTo(3);
    }

    @Test
    void getEmptySlots_airItems_countedAsEmpty() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        ItemStack[] items = new ItemStack[2];
        ItemStack air1 = mock(ItemStack.class);
        when(air1.getType()).thenReturn(Material.AIR);
        items[0] = air1;
        ItemStack air2 = mock(ItemStack.class);
        when(air2.getType()).thenReturn(Material.AIR);
        items[1] = air2;
        when(inventory.getStorageContents()).thenReturn(items);

        assertThat(PlayerUtils.getEmptySlots(player)).isEqualTo(2);
    }

    // --- getPseudoFromSession ---

    @Test
    void getPseudoFromSession_invalidUuid_returnsEmpty() {
        String result = PlayerUtils.getPseudoFromSession("invalid-uuid");

        assertThat(result).isEmpty();
    }
}
