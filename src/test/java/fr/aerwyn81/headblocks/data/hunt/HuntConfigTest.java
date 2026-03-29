package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HuntConfigTest {

    @Mock
    ConfigService configService;

    private HuntConfig config;

    @BeforeEach
    void setUp() {
        config = new HuntConfig(configService);
    }

    // =========================================================================
    // HeadClick category -- representative getters covering String, Boolean,
    // Integer, Double, and List<String> types
    // =========================================================================

    @Nested
    class HeadClick {

        @Test
        void messages_fallsBackToConfigService_whenNoOverride() {
            List<String> global = List.of("Global msg");
            when(configService.headClickMessages()).thenReturn(global);

            assertThat(config.getHeadClickMessages()).isEqualTo(global);
        }

        @Test
        void messages_returnsOverride_whenSet() {
            List<String> custom = List.of("Custom msg");
            config.setHeadClickMessages(custom);

            assertThat(config.getHeadClickMessages()).isEqualTo(custom);
        }

        @Test
        void titleEnabled_fallsBackToConfigService_whenNoOverride() {
            when(configService.headClickTitleEnabled()).thenReturn(true);

            assertThat(config.isHeadClickTitleEnabled()).isTrue();
        }

        @Test
        void titleEnabled_returnsOverride_whenSet() {
            config.setHeadClickTitleEnabled(false);

            assertThat(config.isHeadClickTitleEnabled()).isFalse();
        }

        @Test
        void titleFadeIn_fallsBackToConfigService_whenNoOverride() {
            when(configService.headClickTitleFadeIn()).thenReturn(5);

            assertThat(config.getHeadClickTitleFadeIn()).isEqualTo(5);
        }

        @Test
        void titleFadeIn_returnsOverride_whenSet() {
            config.setHeadClickTitleFadeIn(20);

            assertThat(config.getHeadClickTitleFadeIn()).isEqualTo(20);
        }

        @Test
        void soundFound_fallsBackToConfigService_whenNoOverride() {
            when(configService.headClickNotOwnSound()).thenReturn("BLOCK_NOTE_BLOCK_PLING");

            assertThat(config.getHeadClickSoundFound()).isEqualTo("BLOCK_NOTE_BLOCK_PLING");
        }

        @Test
        void soundFound_returnsOverride_whenSet() {
            config.setHeadClickSoundFound("ENTITY_PLAYER_LEVELUP");

            assertThat(config.getHeadClickSoundFound()).isEqualTo("ENTITY_PLAYER_LEVELUP");
        }

        @Test
        void ejectPower_fallsBackToConfigService_whenNoOverride() {
            when(configService.headClickEjectPower()).thenReturn(1.5);

            assertThat(config.getHeadClickEjectPower()).isEqualTo(1.5);
        }

        @Test
        void ejectPower_returnsOverride_whenSet() {
            config.setHeadClickEjectPower(3.0);

            assertThat(config.getHeadClickEjectPower()).isEqualTo(3.0);
        }
    }

    // =========================================================================
    // Holograms -- special derived logic for isHologramsEnabled
    // =========================================================================

    @Nested
    class Holograms {

        @Test
        void isHologramsEnabled_returnsOverride_whenExplicitlySet() {
            config.setHologramsEnabled(true);

            assertThat(config.isHologramsEnabled()).isTrue();
        }

        @Test
        void isHologramsEnabled_returnsTrue_whenFoundEnabledIsTrue() {
            when(configService.hologramsFoundEnabled()).thenReturn(true);

            assertThat(config.isHologramsEnabled()).isTrue();
        }

        @Test
        void isHologramsEnabled_returnsTrue_whenNotFoundEnabledIsTrue() {
            when(configService.hologramsFoundEnabled()).thenReturn(false);
            when(configService.hologramsNotFoundEnabled()).thenReturn(true);

            assertThat(config.isHologramsEnabled()).isTrue();
        }

        @Test
        void isHologramsEnabled_returnsFalse_whenBothFoundAndNotFoundAreFalse() {
            when(configService.hologramsFoundEnabled()).thenReturn(false);
            when(configService.hologramsNotFoundEnabled()).thenReturn(false);

            assertThat(config.isHologramsEnabled()).isFalse();
        }

        @Test
        void isHologramsEnabled_overrideToFalse_ignoresDerivedLogic() {
            config.setHologramsEnabled(false);

            // Even if sub-flags would be true, the explicit override wins
            assertThat(config.isHologramsEnabled()).isFalse();
        }

        @Test
        void foundLines_fallsBackToConfigService_whenNoOverride() {
            ArrayList<String> fallback = new ArrayList<>(List.of("Default found"));
            when(configService.hologramsFoundLines()).thenReturn(fallback);

            assertThat(config.getHologramsFoundLines()).containsExactly("Default found");
        }

        @Test
        void foundLines_returnsOverride_whenSet() {
            config.setHologramsFoundLines(new ArrayList<>(List.of("Custom found")));

            assertThat(config.getHologramsFoundLines()).containsExactly("Custom found");
        }
    }

    // =========================================================================
    // Hints -- special fallback: configService.hintDistanceBlocks() > 0
    // =========================================================================

    @Nested
    class Hints {

        @Test
        void isHintsEnabled_returnsOverride_whenSet() {
            config.setHintsEnabled(true);

            assertThat(config.isHintsEnabled()).isTrue();
        }

        @Test
        void isHintsEnabled_returnsTrue_whenHintDistancePositive() {
            when(configService.hintDistanceBlocks()).thenReturn(10);

            assertThat(config.isHintsEnabled()).isTrue();
        }

        @Test
        void isHintsEnabled_returnsFalse_whenHintDistanceIsZero() {
            when(configService.hintDistanceBlocks()).thenReturn(0);

            assertThat(config.isHintsEnabled()).isFalse();
        }

        @Test
        void isHintsEnabled_returnsFalse_whenHintDistanceIsNegative() {
            when(configService.hintDistanceBlocks()).thenReturn(-5);

            assertThat(config.isHintsEnabled()).isFalse();
        }

        @Test
        void hintDistance_fallsBackToConfigService_whenNoOverride() {
            when(configService.hintDistanceBlocks()).thenReturn(30);

            assertThat(config.getHintDistance()).isEqualTo(30);
        }

        @Test
        void hintDistance_returnsOverride_whenSet() {
            config.setHintDistance(50);

            assertThat(config.getHintDistance()).isEqualTo(50);
        }

        @Test
        void hintFrequency_fallsBackToConfigService_whenNoOverride() {
            when(configService.hintFrequency()).thenReturn(10);

            assertThat(config.getHintFrequency()).isEqualTo(10);
        }

        @Test
        void hintFrequency_returnsOverride_whenSet() {
            config.setHintFrequency(5);

            assertThat(config.getHintFrequency()).isEqualTo(5);
        }
    }

    // =========================================================================
    // Spin
    // =========================================================================

    @Nested
    class Spin {

        @Test
        void isSpinEnabled_fallsBackToConfigService_whenNoOverride() {
            when(configService.spinEnabled()).thenReturn(true);

            assertThat(config.isSpinEnabled()).isTrue();
        }

        @Test
        void isSpinEnabled_returnsOverride_whenSet() {
            config.setSpinEnabled(false);

            assertThat(config.isSpinEnabled()).isFalse();
        }

        @Test
        void spinSpeed_fallsBackToConfigService_whenNoOverride() {
            when(configService.spinSpeed()).thenReturn(2);

            assertThat(config.getSpinSpeed()).isEqualTo(2);
        }

        @Test
        void spinSpeed_returnsOverride_whenSet() {
            config.setSpinSpeed(8);

            assertThat(config.getSpinSpeed()).isEqualTo(8);
        }
    }

    // =========================================================================
    // Particles
    // =========================================================================

    @Nested
    class Particles {

        @Test
        void foundEnabled_fallsBackToConfigService_whenNoOverride() {
            when(configService.particlesFoundEnabled()).thenReturn(true);

            assertThat(config.isParticlesFoundEnabled()).isTrue();
        }

        @Test
        void foundEnabled_returnsOverride_whenSet() {
            config.setParticlesFoundEnabled(false);

            assertThat(config.isParticlesFoundEnabled()).isFalse();
        }

        @Test
        void foundType_fallsBackToConfigService_whenNoOverride() {
            when(configService.particlesFoundType()).thenReturn("HEART");

            assertThat(config.getParticlesFoundType()).isEqualTo("HEART");
        }

        @Test
        void foundType_returnsOverride_whenSet() {
            config.setParticlesFoundType("FLAME");

            assertThat(config.getParticlesFoundType()).isEqualTo("FLAME");
        }

        @Test
        void notFoundAmount_fallsBackToConfigService_whenNoOverride() {
            when(configService.particlesNotFoundAmount()).thenReturn(3);

            assertThat(config.getParticlesNotFoundAmount()).isEqualTo(3);
        }

        @Test
        void notFoundAmount_returnsOverride_whenSet() {
            config.setParticlesNotFoundAmount(7);

            assertThat(config.getParticlesNotFoundAmount()).isEqualTo(7);
        }
    }

    // =========================================================================
    // TieredRewards
    // =========================================================================

    @Nested
    class TieredRewards {

        @Test
        void fallsBackToConfigService_whenNoOverride() {
            List<TieredReward> global = List.of(
                    new TieredReward(5, List.of("a"), List.of("b"), List.of("c"), 2, true));
            when(configService.tieredRewards()).thenReturn(global);

            assertThat(config.getTieredRewards()).isEqualTo(global);
        }

        @Test
        void returnsOverride_whenSet() {
            List<TieredReward> custom = List.of(
                    new TieredReward(1, List.of("msg"), List.of("cmd"), List.of(), 1, false));
            config.setTieredRewards(custom);

            assertThat(config.getTieredRewards()).isEqualTo(custom);
        }
    }

    // =========================================================================
    // has*() methods -- raw null checks
    // =========================================================================

    @Nested
    class HasMethods {

        @Test
        void hasHeadClickMessages_falseByDefault() {
            assertThat(config.hasHeadClickMessages()).isFalse();
        }

        @Test
        void hasHeadClickMessages_trueWhenSet() {
            config.setHeadClickMessages(List.of("msg"));

            assertThat(config.hasHeadClickMessages()).isTrue();
        }

        @Test
        void hasHologramsFoundLines_falseByDefault() {
            assertThat(config.hasHologramsFoundLines()).isFalse();
        }

        @Test
        void hasHologramsFoundLines_trueWhenSet() {
            config.setHologramsFoundLines(new ArrayList<>(List.of("line")));

            assertThat(config.hasHologramsFoundLines()).isTrue();
        }

        @Test
        void hasHologramsNotFoundLines_falseByDefault() {
            assertThat(config.hasHologramsNotFoundLines()).isFalse();
        }

        @Test
        void hasHologramsNotFoundLines_trueWhenSet() {
            config.setHologramsNotFoundLines(new ArrayList<>(List.of("line")));

            assertThat(config.hasHologramsNotFoundLines()).isTrue();
        }

        @Test
        void hasTieredRewards_falseByDefault() {
            assertThat(config.hasTieredRewards()).isFalse();
        }

        @Test
        void hasTieredRewards_trueWhenSet() {
            config.setTieredRewards(List.of());

            assertThat(config.hasTieredRewards()).isTrue();
        }

        @Test
        void hasSpinConfig_falseByDefault() {
            assertThat(config.hasSpinConfig()).isFalse();
        }

        @Test
        void hasSpinConfig_trueWhenSet() {
            config.setSpinEnabled(false);

            assertThat(config.hasSpinConfig()).isTrue();
        }

        @Test
        void hasHintsConfig_falseByDefault() {
            assertThat(config.hasHintsConfig()).isFalse();
        }

        @Test
        void hasHintsConfig_trueWhenSet() {
            config.setHintsEnabled(true);

            assertThat(config.hasHintsConfig()).isTrue();
        }

        @Test
        void hasParticlesConfig_falseByDefault() {
            assertThat(config.hasParticlesConfig()).isFalse();
        }

        @Test
        void hasParticlesConfig_trueWhenFoundEnabledSet() {
            config.setParticlesFoundEnabled(true);

            assertThat(config.hasParticlesConfig()).isTrue();
        }

        @Test
        void hasParticlesConfig_trueWhenNotFoundEnabledSet() {
            config.setParticlesNotFoundEnabled(false);

            assertThat(config.hasParticlesConfig()).isTrue();
        }

        @Test
        void hasParticlesConfig_trueWhenBothSet() {
            config.setParticlesFoundEnabled(true);
            config.setParticlesNotFoundEnabled(true);

            assertThat(config.hasParticlesConfig()).isTrue();
        }
    }
}
