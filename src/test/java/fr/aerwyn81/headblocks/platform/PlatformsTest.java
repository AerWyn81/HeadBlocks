package fr.aerwyn81.headblocks.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformsTest {

    @Test
    @DisplayName("load() resolves the provider declared by the packaged source set")
    void load_resolves_the_declared_provider() {
        Platform platform = Platforms.load();

        assertThat(platform).isInstanceOf(TestPlatform.class);
        assertThat(platform.name()).isEqualTo("Test");
    }

    @Test
    @DisplayName("load() fails loudly when no provider is packaged")
    void load_throws_when_no_provider_is_packaged() {
        ClassLoader empty = new URLClassLoader(new URL[0], null);

        assertThatThrownBy(() -> Platforms.load(empty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Expected exactly one Platform provider, found []")
                .hasMessageContaining("paperJar or spigotJar");
    }

    @Test
    @DisplayName("load() returns a fresh instance per call")
    void load_returns_a_fresh_instance() {
        assertThat(Platforms.load()).isNotSameAs(Platforms.load());
    }
}
