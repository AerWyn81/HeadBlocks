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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperPlatformTest {

    @Mock
    private Entity entity;

    @Mock
    private Location location;

    @Test
    @DisplayName("the Paper build resolves PaperPlatform")
    void service_declaration_resolves_paper_platform() {
        Platform platform = Platforms.load();

        assertThat(platform).isInstanceOf(PaperPlatform.class);
        assertThat(platform.name()).isEqualTo("Paper");
    }

    @Test
    @DisplayName("teleportAsync delegates to Paper's async teleport")
    void teleport_async_delegates_to_paper_api() {
        CompletableFuture<Boolean> expected = CompletableFuture.completedFuture(true);
        when(entity.teleportAsync(location)).thenReturn(expected);

        CompletableFuture<Boolean> result = new PaperPlatform().teleportAsync(entity, location);

        assertThat(result).isSameAs(expected);
        verify(entity).teleportAsync(location);
        verify(entity, never()).teleport(any(Location.class));
    }
}
