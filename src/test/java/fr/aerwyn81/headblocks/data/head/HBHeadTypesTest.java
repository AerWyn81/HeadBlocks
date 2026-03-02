package fr.aerwyn81.headblocks.data.head;

import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HBHeadTypesTest {

    // --- HeadType enum ---

    @Test
    void headType_hasThreeValues() {
        assertThat(HeadType.values()).containsExactly(HeadType.DEFAULT, HeadType.PLAYER, HeadType.HDB);
    }

    @Test
    void headType_valueOf_default() {
        assertThat(HeadType.valueOf("DEFAULT")).isEqualTo(HeadType.DEFAULT);
    }

    @Test
    void headType_valueOf_player() {
        assertThat(HeadType.valueOf("PLAYER")).isEqualTo(HeadType.PLAYER);
    }

    @Test
    void headType_valueOf_hdb() {
        assertThat(HeadType.valueOf("HDB")).isEqualTo(HeadType.HDB);
    }

    // --- HBHead base class ---

    @Test
    void hbHead_getSetItemStack() {
        HBHead head = new HBHead();
        ItemStack item = mock(ItemStack.class);

        head.setItemStack(item);

        assertThat(head.getItemStack()).isSameAs(item);
    }

    @Test
    void hbHead_defaultItemStack_isNull() {
        HBHead head = new HBHead();

        assertThat(head.getItemStack()).isNull();
    }

    // --- HBHeadDefault ---

    @Test
    void hbHeadDefault_constructorSetsItemStack() {
        ItemStack item = mock(ItemStack.class);
        HBHeadDefault head = new HBHeadDefault(item);

        assertThat(head.getItemStack()).isSameAs(item);
    }

    @Test
    void hbHeadDefault_getType_returnsDefault() {
        HBHeadDefault head = new HBHeadDefault(mock(ItemStack.class));

        assertThat(head.getType()).isEqualTo(HeadType.DEFAULT);
    }

    @Test
    void hbHeadDefault_getSetTexture() {
        HBHeadDefault head = new HBHeadDefault(mock(ItemStack.class));

        head.setTexture("abc123");

        assertThat(head.getTexture()).isEqualTo("abc123");
    }

    @Test
    void hbHeadDefault_texture_defaultNull() {
        HBHeadDefault head = new HBHeadDefault(mock(ItemStack.class));

        assertThat(head.getTexture()).isNull();
    }

    // --- HBHeadPlayer ---

    @Test
    void hbHeadPlayer_constructorSetsItemStack() {
        ItemStack item = mock(ItemStack.class);
        HBHeadPlayer head = new HBHeadPlayer(item);

        assertThat(head.getItemStack()).isSameAs(item);
    }

    @Test
    void hbHeadPlayer_getType_returnsPlayer() {
        HBHeadPlayer head = new HBHeadPlayer(mock(ItemStack.class));

        assertThat(head.getType()).isEqualTo(HeadType.PLAYER);
    }

    // --- HBHeadHDB ---

    @Test
    void hbHeadHDB_constructorSetsItemStackAndId() {
        ItemStack item = mock(ItemStack.class);
        HBHeadHDB head = new HBHeadHDB(item, "hdb-123");

        assertThat(head.getItemStack()).isSameAs(item);
        assertThat(head.getId()).isEqualTo("hdb-123");
    }

    @Test
    void hbHeadHDB_getType_returnsHDB() {
        HBHeadHDB head = new HBHeadHDB(mock(ItemStack.class), "id");

        assertThat(head.getType()).isEqualTo(HeadType.HDB);
    }

    @Test
    void hbHeadHDB_defaultNotLoaded() {
        HBHeadHDB head = new HBHeadHDB(mock(ItemStack.class), "id");

        assertThat(head.isLoaded()).isFalse();
    }

    @Test
    void hbHeadHDB_setLoaded() {
        HBHeadHDB head = new HBHeadHDB(mock(ItemStack.class), "id");

        head.setLoaded(true);

        assertThat(head.isLoaded()).isTrue();
    }

    @Test
    void hbHeadHDB_setId() {
        HBHeadHDB head = new HBHeadHDB(mock(ItemStack.class), "old");

        head.setId("new-id");

        assertThat(head.getId()).isEqualTo("new-id");
    }
}
