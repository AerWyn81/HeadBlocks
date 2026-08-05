package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.Particle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1.20.5 renamed a batch of particle constants. Configs written before that release still hold the
 * old names, so resolution has to accept both spellings whatever the server runs.
 */
class ParticlesUtilsResolveTest {

    @Test
    @DisplayName("the pre-1.20.5 name REDSTONE resolves, which is the default in config.yml")
    void legacy_redstone_resolves() {
        assertThat(ParticlesUtils.resolve("REDSTONE")).isNotNull();
    }

    @Test
    @DisplayName("REDSTONE and DUST resolve to the same particle")
    void redstone_and_dust_are_the_same_particle() {
        assertThat(ParticlesUtils.resolve("REDSTONE")).isSameAs(ParticlesUtils.resolve("DUST"));
    }

    @Test
    @DisplayName("VILLAGER_HAPPY and HAPPY_VILLAGER resolve to the same particle")
    void villager_happy_resolves_both_ways() {
        assertThat(ParticlesUtils.resolve("VILLAGER_HAPPY")).isSameAs(ParticlesUtils.resolve("HAPPY_VILLAGER"));
    }

    @Test
    @DisplayName("a particle that never changed name still resolves")
    void stable_names_resolve() {
        assertThat(ParticlesUtils.resolve("FLAME")).isEqualTo(Particle.FLAME);
    }

    @Test
    @DisplayName("resolution is case-insensitive, as config values are not normalised")
    void resolution_is_case_insensitive() {
        assertThat(ParticlesUtils.resolve("redstone")).isSameAs(ParticlesUtils.resolve("REDSTONE"));
    }

    @Test
    @DisplayName("an unknown name fails with the offending value, not a bare enum error")
    void unknown_name_is_reported() {
        assertThatThrownBy(() -> ParticlesUtils.resolve("NOT_A_PARTICLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOT_A_PARTICLE");
    }
}
