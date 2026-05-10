package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHeadDB;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadProviderHookTest {

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private SchedulerAdapter scheduler;

    @Test
    void headDatabaseHook_prefix_isHdb() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);

        assertThat(hook.prefix()).isEqualTo("hdb");
    }

    @Test
    void headDatabaseHook_isAvailable_delegatesToPluginProvider() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);
        when(pluginProvider.isHeadDatabaseActive()).thenReturn(true);

        assertThat(hook.isAvailable()).isTrue();
    }

    @Test
    void headDatabaseHook_isAvailable_falseWhenPluginInactive() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);
        when(pluginProvider.isHeadDatabaseActive()).thenReturn(false);

        assertThat(hook.isAvailable()).isFalse();
    }

    @Test
    void headDatabaseHook_createHead_returnsHBHeadHDB() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);
        ItemStack base = mock(ItemStack.class);

        HBHead head = hook.createHead(base, "1234");

        assertThat(head).isInstanceOf(HBHeadHDB.class);
        assertThat(((HBHeadHDB) head).getId()).isEqualTo("1234");
        assertThat(head.getItemStack()).isSameAs(base);
    }

    @Test
    void headDatabaseHook_createHead_acceptsArbitraryStringId() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);
        ItemStack base = mock(ItemStack.class);

        HBHead head = hook.createHead(base, "alpha-1");

        assertThat(((HBHeadHDB) head).getId()).isEqualTo("alpha-1");
    }

    @Test
    void headDBHook_prefix_isHeaddb() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);

        assertThat(hook.prefix()).isEqualTo("headdb");
    }

    @Test
    void headDBHook_isAvailable_delegatesToPluginProvider() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);
        lenient().when(pluginProvider.isHeadDBActive()).thenReturn(true);

        assertThat(hook.isAvailable()).isTrue();
    }

    @Test
    void headDBHook_isAvailable_falseWhenPluginInactive() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);
        lenient().when(pluginProvider.isHeadDBActive()).thenReturn(false);

        assertThat(hook.isAvailable()).isFalse();
    }

    @Test
    void headDBHook_createHead_returnsHBHeadHeadDBWithParsedId() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);
        ItemStack base = mock(ItemStack.class);

        HBHead head = hook.createHead(base, "5678");

        assertThat(head).isInstanceOf(HBHeadHeadDB.class);
        assertThat(((HBHeadHeadDB) head).getId()).isEqualTo(5678);
        assertThat(head.getItemStack()).isSameAs(base);
    }

    @Test
    void headDBHook_createHead_trimsWhitespace() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);
        ItemStack base = mock(ItemStack.class);

        HBHead head = hook.createHead(base, "  42  ");

        assertThat(((HBHeadHeadDB) head).getId()).isEqualTo(42);
    }

    @Test
    void headDBHook_createHead_throwsOnNonNumericId() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);
        ItemStack base = mock(ItemStack.class);

        assertThatThrownBy(() -> hook.createHead(base, "not-a-number"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HeadDB id must be a number");
    }

    @Test
    void headDBHook_loadTextures_isNoOpWhenNotInitialized() {
        HeadDBHook hook = new HeadDBHook(pluginProvider, scheduler);

        hook.loadTextures();
    }

    @Test
    void headDBHook_toBase64Texture_wrapsRawHash() {
        String hash = "5e61873d47faa5e67939e964640b425faa7690d6b5dea8528b25ca7110eda1";

        String base64 = HeadDBHook.toBase64Texture(hash);
        String decoded = new String(java.util.Base64.getDecoder().decode(base64));

        assertThat(decoded).contains("http://textures.minecraft.net/texture/" + hash);
        assertThat(decoded).contains("\"SKIN\"");
    }

    @Test
    void headDatabaseHook_loadTextures_isNoOpWhenNotInitialized() {
        HeadDatabaseHook hook = new HeadDatabaseHook(pluginProvider);

        hook.loadTextures();
    }
}
