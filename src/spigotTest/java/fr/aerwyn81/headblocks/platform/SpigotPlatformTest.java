package fr.aerwyn81.headblocks.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpigotPlatformTest {

    @Mock
    private Entity entity;

    @Mock
    private Location location;

    @Test
    @DisplayName("the Spigot build resolves SpigotPlatform")
    void service_declaration_resolves_spigot_platform() {
        Platform platform = Platforms.load();

        assertThat(platform).isInstanceOf(SpigotPlatform.class);
        assertThat(platform.name()).isEqualTo("Spigot");
    }

    @Test
    @DisplayName("teleportAsync completes synchronously with the teleport outcome")
    void teleport_async_wraps_the_synchronous_teleport() {
        when(entity.teleport(location)).thenReturn(true);

        CompletableFuture<Boolean> result = new SpigotPlatform().teleportAsync(entity, location);

        assertThat(result).isCompletedWithValue(true);
        verify(entity).teleport(location);
    }
}
