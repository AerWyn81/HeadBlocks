package fr.aerwyn81.headblocks.services;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import org.bukkit.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import redis.clients.jedis.Protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- Helper ---

    private java.io.File writeConfig(String yaml) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, yaml);
        return configFile.toFile();
    }

    // =========================================================================
    // 1. Defaults
    // =========================================================================

    @Nested
    class Defaults {

        @Test
        void language_defaults_to_en() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.language()).isEqualTo("en");
        }

        @Test
        void metricsEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.metricsEnabled()).isTrue();
        }

        @Test
        void resetPlayerData_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.resetPlayerData()).isTrue();
        }

        @Test
        void hideFoundHeads_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.isHideFoundHeads()).isFalse();
        }

        @Test
        void heads_defaults_to_empty_list() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.heads()).isEmpty();
        }

        @Test
        void preventCommandsOnTieredRewardsLevel_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.preventCommandsOnTieredRewardsLevel()).isFalse();
        }

        @Test
        void preventMessagesOnTieredRewardsLevel_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.preventMessagesOnTieredRewardsLevel()).isFalse();
        }

        @Test
        void headsThemeEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headsThemeEnabled()).isFalse();
        }

        @Test
        void headsThemeSelected_defaults_to_empty_string() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headsThemeSelected()).isEmpty();
        }

        @Test
        void headClickAlreadyOwnSound_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickAlreadyOwnSound()).isNull();
        }

        @Test
        void headClickNotOwnSound_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickNotOwnSound()).isNull();
        }

        @Test
        void headClickTitleFirstLine_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleFirstLine()).isEmpty();
        }

        @Test
        void headClickTitleSubTitle_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleSubTitle()).isEmpty();
        }

        @Test
        void headClickTitleFadeIn_defaults_to_0() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleFadeIn()).isZero();
        }

        @Test
        void headClickTitleStay_defaults_to_50() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleStay()).isEqualTo(50);
        }

        @Test
        void headClickTitleFadeOut_defaults_to_0() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleFadeOut()).isZero();
        }

        @Test
        void fireworkFlickerEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.fireworkFlickerEnabled()).isTrue();
        }

        @Test
        void headClickFireworkPower_defaults_to_0() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickFireworkPower()).isZero();
        }

        @Test
        void headClickParticlesEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickParticlesEnabled()).isFalse();
        }

        @Test
        void headClickParticlesAlreadyOwnType_defaults_to_VILLAGER_ANGRY() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickParticlesAlreadyOwnType()).isEqualTo("VILLAGER_ANGRY");
        }

        @Test
        void headClickParticlesAmount_defaults_to_1() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickParticlesAmount()).isEqualTo(1);
        }

        @Test
        void headClickParticlesColors_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickParticlesColors()).isEmpty();
        }

        @Test
        void headClickEjectEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickEjectEnabled()).isFalse();
        }

        @Test
        void headClickEjectPower_defaults_to_1() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickEjectPower()).isEqualTo(1D);
        }

        @Test
        void progressBarSymbol_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.progressBarSymbol()).isNull();
        }

        @Test
        void progressBarNotCompletedColor_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.progressBarNotCompletedColor()).isNull();
        }

        @Test
        void progressBarCompletedColor_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.progressBarCompletedColor()).isNull();
        }

        @Test
        void hologramPlugin_defaults_to_null() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramPlugin()).isNull();
        }

        @Test
        void hologramsHeightAboveHead_defaults_to_0_5() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramsHeightAboveHead()).isEqualTo(0.5);
        }

        @Test
        void hologramAdvancedFoundPlaceholder_defaults_to_green_found() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramAdvancedFoundPlaceholder()).isEqualTo("&a&lFound");
        }

        @Test
        void hologramAdvancedNotFoundPlaceholder_defaults_to_red_not_found() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramAdvancedNotFoundPlaceholder()).isEqualTo("&c&lNot found");
        }

        @Test
        void hologramParticlePlayerViewDistance_defaults_to_16() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramParticlePlayerViewDistance()).isEqualTo(16);
        }

        @Test
        void delayGlobalTask_defaults_to_20() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.delayGlobalTask()).isEqualTo(20);
        }

        @Test
        void preventPistonExtension_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.preventPistonExtension()).isTrue();
        }

        @Test
        void preventLiquidFlow_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.preventLiquidFlow()).isTrue();
        }

        @Test
        void preventExplosion_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.preventExplosion()).isTrue();
        }

        @Test
        void placeholdersLeaderboardPrefix_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.placeholdersLeaderboardPrefix()).isEmpty();
        }

        @Test
        void placeholdersLeaderboardSuffix_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.placeholdersLeaderboardSuffix()).isEmpty();
        }

        @Test
        void placeholdersLeaderboardUseNickname_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.placeholdersLeaderboardUseNickname()).isFalse();
        }

        @Test
        void hintSoundVolume_defaults_to_1() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hintSoundVolume()).isEqualTo(1);
        }

        @Test
        void hintActionBarMessage_defaults_to_expected_value() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hintActionBarMessage()).isEqualTo("%prefix% &aPssst, a mystery block is near! &7(%arrow%)");
        }

        @Test
        void particlesFoundType_defaults_to_REDSTONE() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesFoundType()).isEqualTo("REDSTONE");
        }

        @Test
        void particlesNotFoundType_defaults_to_REDSTONE() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesNotFoundType()).isEqualTo("REDSTONE");
        }

        @Test
        void particlesFoundAmount_defaults_to_3() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesFoundAmount()).isEqualTo(3);
        }

        @Test
        void particlesNotFoundAmount_defaults_to_3() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesNotFoundAmount()).isEqualTo(3);
        }

        @Test
        void particlesFoundColors_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesFoundColors()).isEmpty();
        }

        @Test
        void particlesNotFoundColors_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesNotFoundColors()).isEmpty();
        }

        @Test
        void particlesFoundEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesFoundEnabled()).isTrue();
        }

        @Test
        void particlesNotFoundEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.particlesNotFoundEnabled()).isFalse();
        }

        @Test
        void hologramsFoundEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramsFoundEnabled()).isTrue();
        }

        @Test
        void hologramsNotFoundEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramsNotFoundEnabled()).isTrue();
        }

        @Test
        void databaseHostname_defaults_to_localhost() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseHostname()).isEqualTo("localhost");
        }

        @Test
        void databasePrefix_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databasePrefix()).isEmpty();
        }

        @Test
        void databaseMaxConnections_defaults_to_10() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseMaxConnections()).isEqualTo(10);
        }

        @Test
        void databaseMinIdleConnections_defaults_to_2() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseMinIdleConnections()).isEqualTo(2);
        }

        @Test
        void databaseIdleTimeout_default_is_300_seconds() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseIdleTimeout()).isEqualTo(300_000L);
        }

        @Test
        void databaseMaxLifetime_default_is_1800_seconds() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseMaxLifetime()).isEqualTo(1_800_000L);
        }
    }

    // =========================================================================
    // 2. Custom values
    // =========================================================================

    @Nested
    class CustomValues {

        @Test
        void language_returns_custom_value_lowercased() throws IOException {
            var service = new ConfigService(writeConfig("language: FR"));

            assertThat(service.language()).isEqualTo("fr");
        }

        @Test
        void metricsEnabled_returns_false_when_configured() throws IOException {
            var service = new ConfigService(writeConfig("metrics: false"));

            assertThat(service.metricsEnabled()).isFalse();
        }

        @Test
        void heads_returns_configured_list() throws IOException {
            String yaml = """
                    heads:
                      - "head_texture_1"
                      - "head_texture_2"
                      - "head_texture_3"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.heads()).containsExactly("head_texture_1", "head_texture_2", "head_texture_3");
        }

        @Test
        void resetPlayerData_returns_false_when_configured() throws IOException {
            var service = new ConfigService(writeConfig("shouldResetPlayerData: false"));

            assertThat(service.resetPlayerData()).isFalse();
        }

        @Test
        void hideFoundHeads_returns_true_when_configured() throws IOException {
            var service = new ConfigService(writeConfig("hideFoundHeads: true"));

            assertThat(service.isHideFoundHeads()).isTrue();
        }

        @Test
        void preventCommandsOnTieredRewardsLevel_returns_true_when_configured() throws IOException {
            var service = new ConfigService(writeConfig("preventCommandsOnTieredRewardsLevel: true"));

            assertThat(service.preventCommandsOnTieredRewardsLevel()).isTrue();
        }

        @Test
        void preventMessagesOnTieredRewardsLevel_returns_true_when_configured() throws IOException {
            var service = new ConfigService(writeConfig("preventMessagesOnTieredRewardsLevel: true"));

            assertThat(service.preventMessagesOnTieredRewardsLevel()).isTrue();
        }
    }

    // =========================================================================
    // 3. HeadClick
    // =========================================================================

    @Nested
    class HeadClick {

        @Test
        void headClickMessages_returns_configured_list() throws IOException {
            String yaml = """
                    headClick:
                      messages:
                        - "&aYou found a head!"
                        - "&bNice job!"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickMessages()).containsExactly("&aYou found a head!", "&bNice job!");
        }

        @Test
        void headClickMessages_empty_when_not_configured() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickMessages()).isEmpty();
        }

        @Test
        void headClickTitleEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickTitleEnabled()).isFalse();
        }

        @Test
        void headClickTitleEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleEnabled()).isTrue();
        }

        @Test
        void headClickTitleFirstLine_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        firstLine: "&aHead Found!"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleFirstLine()).isEqualTo("&aHead Found!");
        }

        @Test
        void headClickTitleSubTitle_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        subTitle: "&7Keep going!"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleSubTitle()).isEqualTo("&7Keep going!");
        }

        @Test
        void headClickTitleFadeIn_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        fadeIn: 10
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleFadeIn()).isEqualTo(10);
        }

        @Test
        void headClickTitleStay_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        stay: 100
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleStay()).isEqualTo(100);
        }

        @Test
        void headClickTitleFadeOut_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      title:
                        fadeOut: 20
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickTitleFadeOut()).isEqualTo(20);
        }

        @Test
        void headClickCommands_returns_configured_list() throws IOException {
            String yaml = """
                    headClick:
                      commands:
                        - "give %player% diamond 1"
                        - "eco give %player% 100"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickCommands()).containsExactly("give %player% diamond 1", "eco give %player% 100");
        }

        @Test
        void headClickCommands_empty_when_not_configured() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickCommands()).isEmpty();
        }

        @Test
        void headClickCommandsRandomized_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickCommandsRandomized()).isFalse();
        }

        @Test
        void headClickCommandsRandomized_returns_true_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      randomizeCommands: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickCommandsRandomized()).isTrue();
        }

        @Test
        void headClickCommandsSlotsRequired_defaults_to_minus_one() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickCommandsSlotsRequired()).isEqualTo(-1);
        }

        @Test
        void headClickCommandsSlotsRequired_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      slotsRequired: 5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickCommandsSlotsRequired()).isEqualTo(5);
        }

        @Test
        void headClickAlreadyOwnSound_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      sounds:
                        alreadyOwn: "ENTITY_VILLAGER_NO"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickAlreadyOwnSound()).isEqualTo("ENTITY_VILLAGER_NO");
        }

        @Test
        void headClickNotOwnSound_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      sounds:
                        notOwn: "ENTITY_PLAYER_LEVELUP"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickNotOwnSound()).isEqualTo("ENTITY_PLAYER_LEVELUP");
        }

        @Test
        void headClickParticlesEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      particles:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickParticlesEnabled()).isTrue();
        }

        @Test
        void headClickParticlesAlreadyOwnType_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      particles:
                        alreadyOwn:
                          type: "FLAME"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickParticlesAlreadyOwnType()).isEqualTo("FLAME");
        }

        @Test
        void headClickParticlesAmount_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      particles:
                        alreadyOwn:
                          amount: 5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickParticlesAmount()).isEqualTo(5);
        }

        @Test
        void headClickParticlesColors_returns_configured_list() throws IOException {
            String yaml = """
                    headClick:
                      particles:
                        alreadyOwn:
                          colors:
                            - "255,0,0"
                            - "0,255,0"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickParticlesColors()).containsExactly("255,0,0", "0,255,0");
        }

        @Test
        void headClickEjectEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      pushBack:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickEjectEnabled()).isTrue();
        }

        @Test
        void headClickEjectPower_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      pushBack:
                        power: 2.5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickEjectPower()).isEqualTo(2.5);
        }
    }

    // =========================================================================
    // 4. Firework colors
    // =========================================================================

    @Nested
    class FireworkColors {

        @Test
        void fireworkEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.fireworkEnabled()).isFalse();
        }

        @Test
        void fireworkEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.fireworkEnabled()).isTrue();
        }

        @Test
        void fireworkFlickerEnabled_returns_false_when_configured() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        flicker: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.fireworkFlickerEnabled()).isFalse();
        }

        @Test
        void headClickFireworkPower_returns_configured_value() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        power: 3
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickFireworkPower()).isEqualTo(3);
        }

        @Test
        void headClickFireworkColors_valid_rgb_format() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        colors:
                          - "255,0,128"
                          - "0,255,0"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<Color> colors = service.headClickFireworkColors();
            assertThat(colors).hasSize(2);
            assertThat(colors.get(0)).isEqualTo(Color.fromRGB(255, 0, 128));
            assertThat(colors.get(1)).isEqualTo(Color.fromRGB(0, 255, 0));
        }

        @Test
        void headClickFireworkColors_invalid_format_returns_empty_for_bad_entries() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        colors:
                          - "not_a_color"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickFireworkColors()).isEmpty();
        }

        @Test
        void headClickFireworkColors_missing_section_returns_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickFireworkColors()).isEmpty();
        }

        @Test
        void headClickFireworkFadeColors_valid_rgb_format() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        fadeColors:
                          - "128,128,128"
                          - "64,64,64"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<Color> colors = service.headClickFireworkFadeColors();
            assertThat(colors).hasSize(2);
            assertThat(colors.get(0)).isEqualTo(Color.fromRGB(128, 128, 128));
            assertThat(colors.get(1)).isEqualTo(Color.fromRGB(64, 64, 64));
        }

        @Test
        void headClickFireworkFadeColors_missing_section_returns_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headClickFireworkFadeColors()).isEmpty();
        }

        @Test
        void headClickFireworkFadeColors_invalid_format_returns_empty_for_bad_entries() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        fadeColors:
                          - "bad_color"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headClickFireworkFadeColors()).isEmpty();
        }

        @Test
        void headClickFireworkColors_mixed_valid_and_invalid_keeps_valid() throws IOException {
            String yaml = """
                    headClick:
                      firework:
                        colors:
                          - "255,0,0"
                          - "invalid"
                          - "0,0,255"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<Color> colors = service.headClickFireworkColors();
            assertThat(colors).hasSize(2);
            assertThat(colors.get(0)).isEqualTo(Color.fromRGB(255, 0, 0));
            assertThat(colors.get(1)).isEqualTo(Color.fromRGB(0, 0, 255));
        }
    }

    // =========================================================================
    // 5. Tiered rewards
    // =========================================================================

    @Nested
    class TieredRewards {

        @Test
        void tieredRewards_single_tier() throws IOException {
            String yaml = """
                    tieredRewards:
                      3:
                        messages:
                          - "You reached level 3!"
                        commands:
                          - "give %player% diamond 1"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<TieredReward> rewards = service.tieredRewards();
            assertThat(rewards).hasSize(1);
            assertThat(rewards.get(0).level()).isEqualTo(3);
            assertThat(rewards.get(0).messages()).containsExactly("You reached level 3!");
            assertThat(rewards.get(0).commands()).containsExactly("give %player% diamond 1");
        }

        @Test
        void tieredRewards_multiple_tiers() throws IOException {
            String yaml = """
                    tieredRewards:
                      5:
                        messages:
                          - "Tier 5!"
                        commands:
                          - "cmd5"
                      10:
                        messages:
                          - "Tier 10!"
                        commands:
                          - "cmd10"
                        broadcast:
                          - "Player reached tier 10!"
                        slotsRequired: 2
                        randomizeCommands: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<TieredReward> rewards = service.tieredRewards();
            assertThat(rewards).hasSize(2);

            TieredReward tier10 = rewards.stream()
                    .filter(r -> r.level() == 10)
                    .findFirst()
                    .orElseThrow();
            assertThat(tier10.messages()).containsExactly("Tier 10!");
            assertThat(tier10.commands()).containsExactly("cmd10");
            assertThat(tier10.broadcastMessages()).containsExactly("Player reached tier 10!");
            assertThat(tier10.slotsRequired()).isEqualTo(2);
            assertThat(tier10.isRandom()).isTrue();
        }

        @Test
        void tieredRewards_empty_section_returns_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.tieredRewards()).isEmpty();
        }

        @Test
        void tieredRewards_invalid_tier_number_skips_entry() throws IOException {
            String yaml = """
                    tieredRewards:
                      notANumber:
                        messages:
                          - "Should be skipped"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.tieredRewards()).isEmpty();
        }

        @Test
        void tieredRewards_tier_with_only_slotsRequired() throws IOException {
            String yaml = """
                    tieredRewards:
                      7:
                        slotsRequired: 3
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<TieredReward> rewards = service.tieredRewards();
            assertThat(rewards).hasSize(1);
            assertThat(rewards.get(0).level()).isEqualTo(7);
            assertThat(rewards.get(0).slotsRequired()).isEqualTo(3);
            assertThat(rewards.get(0).messages()).isEmpty();
            assertThat(rewards.get(0).commands()).isEmpty();
        }

        @Test
        void tieredRewards_tier_with_only_broadcast() throws IOException {
            String yaml = """
                    tieredRewards:
                      4:
                        broadcast:
                          - "Broadcast message!"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<TieredReward> rewards = service.tieredRewards();
            assertThat(rewards).hasSize(1);
            assertThat(rewards.get(0).level()).isEqualTo(4);
            assertThat(rewards.get(0).broadcastMessages()).containsExactly("Broadcast message!");
            assertThat(rewards.get(0).messages()).isEmpty();
            assertThat(rewards.get(0).commands()).isEmpty();
        }

        @Test
        void tieredRewards_tier_with_only_randomize_and_no_content_is_skipped() throws IOException {
            String yaml = """
                    tieredRewards:
                      2:
                        randomizeCommands: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.tieredRewards()).isEmpty();
        }

        @Test
        void tieredRewards_defaults_slotsRequired_to_minus_one_and_isRandom_to_false() throws IOException {
            String yaml = """
                    tieredRewards:
                      1:
                        messages:
                          - "Hello"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            List<TieredReward> rewards = service.tieredRewards();
            assertThat(rewards).hasSize(1);
            assertThat(rewards.get(0).slotsRequired()).isEqualTo(-1);
            assertThat(rewards.get(0).isRandom()).isFalse();
        }
    }

    // =========================================================================
    // 6. Database
    // =========================================================================

    @Nested
    class Database {

        @Test
        void databaseEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseEnabled()).isFalse();
        }

        @Test
        void databaseEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    database:
                      enable: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseEnabled()).isTrue();
        }

        @Test
        void databaseType_defaults_to_MySQL() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseType()).isEqualTo(EnumTypeDatabase.MySQL);
        }

        @Test
        void databaseType_returns_SQLite_when_configured() throws IOException {
            String yaml = """
                    database:
                      type: SQLite
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseType()).isEqualTo(EnumTypeDatabase.SQLite);
        }

        @Test
        void databaseType_invalid_value_falls_back_to_MySQL() throws IOException {
            String yaml = """
                    database:
                      type: Postgres
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseType()).isEqualTo(EnumTypeDatabase.MySQL);
        }

        @Test
        void databaseHostname_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        hostname: "db.example.com"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseHostname()).isEqualTo("db.example.com");
        }

        @Test
        void databaseName_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        database: "headblocks_db"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseName()).isEqualTo("headblocks_db");
        }

        @Test
        void databaseUsername_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        username: "admin"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseUsername()).isEqualTo("admin");
        }

        @Test
        void databasePassword_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        password: "secret123"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databasePassword()).isEqualTo("secret123");
        }

        @Test
        void databasePort_defaults_to_3306() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databasePort()).isEqualTo(3306);
        }

        @Test
        void databasePort_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        port: 5432
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databasePort()).isEqualTo(5432);
        }

        @Test
        void databaseSsl_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseSsl()).isFalse();
        }

        @Test
        void databaseSsl_returns_true_when_configured() throws IOException {
            String yaml = """
                    database:
                      settings:
                        ssl: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseSsl()).isTrue();
        }

        @Test
        void databasePrefix_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        prefix: "myserver_"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databasePrefix()).isEqualTo("myserver_");
        }

        @Test
        void databaseMaxConnections_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        pool:
                          maxConnections: 20
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseMaxConnections()).isEqualTo(20);
        }

        @Test
        void databaseMinIdleConnections_returns_configured_value() throws IOException {
            String yaml = """
                    database:
                      settings:
                        pool:
                          minIdleConnections: 5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseMinIdleConnections()).isEqualTo(5);
        }

        @Test
        void databaseConnectionTimeout_multiplied_by_1000() throws IOException {
            String yaml = """
                    database:
                      settings:
                        pool:
                          connectionTimeout: 10
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseConnectionTimeout()).isEqualTo(10_000L);
        }

        @Test
        void databaseConnectionTimeout_default_is_5_seconds() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.databaseConnectionTimeout()).isEqualTo(5_000L);
        }

        @Test
        void databaseIdleTimeout_multiplied_by_1000() throws IOException {
            String yaml = """
                    database:
                      settings:
                        pool:
                          idleTimeout: 600
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseIdleTimeout()).isEqualTo(600_000L);
        }

        @Test
        void databaseMaxLifetime_multiplied_by_1000() throws IOException {
            String yaml = """
                    database:
                      settings:
                        pool:
                          maxLifetime: 3600
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.databaseMaxLifetime()).isEqualTo(3_600_000L);
        }
    }

    // =========================================================================
    // 7. Redis
    // =========================================================================

    @Nested
    class Redis {

        @Test
        void redisEnabled_defaults_to_false() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.redisEnabled()).isFalse();
        }

        @Test
        void redisEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    redis:
                      enable: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.redisEnabled()).isTrue();
        }

        @Test
        void redisHostname_defaults_to_protocol_default() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.redisHostname()).isEqualTo(Protocol.DEFAULT_HOST);
        }

        @Test
        void redisHostname_returns_configured_value() throws IOException {
            String yaml = """
                    redis:
                      settings:
                        hostname: "redis.example.com"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.redisHostname()).isEqualTo("redis.example.com");
        }

        @Test
        void redisPort_defaults_to_protocol_default() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.redisPort()).isEqualTo(Protocol.DEFAULT_PORT);
        }

        @Test
        void redisPort_returns_configured_value() throws IOException {
            String yaml = """
                    redis:
                      settings:
                        port: 6380
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.redisPort()).isEqualTo(6380);
        }

        @Test
        void redisDatabase_defaults_to_protocol_default() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.redisDatabase()).isEqualTo(Protocol.DEFAULT_DATABASE);
        }

        @Test
        void redisDatabase_returns_configured_value() throws IOException {
            String yaml = """
                    redis:
                      settings:
                        database: 5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.redisDatabase()).isEqualTo(5);
        }

        @Test
        void redisPassword_defaults_to_empty_string() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.redisPassword()).isEmpty();
        }

        @Test
        void redisPassword_returns_configured_value() throws IOException {
            String yaml = """
                    redis:
                      settings:
                        password: "mysecret"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.redisPassword()).isEqualTo("mysecret");
        }
    }

    // =========================================================================
    // 8. Holograms and Particles
    // =========================================================================

    @Nested
    class HologramsAndParticles {

        @Test
        void hologramsEnabled_true_when_found_enabled() throws IOException {
            String yaml = """
                    holograms:
                      found:
                        enabled: true
                      notFound:
                        enabled: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsEnabled()).isTrue();
        }

        @Test
        void hologramsEnabled_true_when_notFound_enabled() throws IOException {
            String yaml = """
                    holograms:
                      found:
                        enabled: false
                      notFound:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsEnabled()).isTrue();
        }

        @Test
        void hologramsEnabled_false_when_both_disabled() throws IOException {
            String yaml = """
                    holograms:
                      found:
                        enabled: false
                      notFound:
                        enabled: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsEnabled()).isFalse();
        }

        @Test
        void hologramsFoundLines_returns_configured_lines() throws IOException {
            String yaml = """
                    holograms:
                      found:
                        lines:
                          - "&aFound!"
                          - "&7Good job"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsFoundLines()).containsExactly("&aFound!", "&7Good job");
        }

        @Test
        void hologramsNotFoundLines_returns_configured_lines() throws IOException {
            String yaml = """
                    holograms:
                      notFound:
                        lines:
                          - "&cNot found yet"
                          - "&7Keep searching!"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsNotFoundLines()).containsExactly("&cNot found yet", "&7Keep searching!");
        }

        @Test
        void hologramsNotFoundLines_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramsNotFoundLines()).isEmpty();
        }

        @Test
        void hologramsAdvancedLines_returns_configured_lines() throws IOException {
            String yaml = """
                    holograms:
                      advanced:
                        lines:
                          - "&eHead #1"
                          - "%status%"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsAdvancedLines()).containsExactly("&eHead #1", "%status%");
        }

        @Test
        void hologramsAdvancedLines_defaults_to_empty() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hologramsAdvancedLines()).isEmpty();
        }

        @Test
        void hologramAdvancedFoundPlaceholder_returns_configured_value() throws IOException {
            String yaml = """
                    holograms:
                      advanced:
                        foundPlaceholder: "&2&lDiscovered"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramAdvancedFoundPlaceholder()).isEqualTo("&2&lDiscovered");
        }

        @Test
        void hologramAdvancedNotFoundPlaceholder_returns_configured_value() throws IOException {
            String yaml = """
                    holograms:
                      advanced:
                        notFoundPlaceholder: "&4&lMissing"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramAdvancedNotFoundPlaceholder()).isEqualTo("&4&lMissing");
        }

        @Test
        void hologramsHeightAboveHead_returns_configured_value() throws IOException {
            String yaml = """
                    holograms:
                      heightAboveHead: 1.5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramsHeightAboveHead()).isEqualTo(1.5);
        }

        @Test
        void hologramPlugin_returns_configured_value() throws IOException {
            String yaml = """
                    holograms:
                      plugin: "HoloEasy"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramPlugin()).isEqualTo("HoloEasy");
        }

        @Test
        void particlesEnabled_true_when_found_enabled() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        enabled: true
                      notFound:
                        enabled: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesEnabled()).isTrue();
        }

        @Test
        void particlesEnabled_true_when_notFound_enabled() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        enabled: false
                      notFound:
                        enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesEnabled()).isTrue();
        }

        @Test
        void particlesEnabled_false_when_both_disabled() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        enabled: false
                      notFound:
                        enabled: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesEnabled()).isFalse();
        }

        @Test
        void particlesFoundType_returns_configured_value() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        type: "FLAME"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesFoundType()).isEqualTo("FLAME");
        }

        @Test
        void particlesNotFoundType_returns_configured_value() throws IOException {
            String yaml = """
                    floatingParticles:
                      notFound:
                        type: "HEART"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesNotFoundType()).isEqualTo("HEART");
        }

        @Test
        void particlesFoundAmount_returns_configured_value() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        amount: 10
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesFoundAmount()).isEqualTo(10);
        }

        @Test
        void particlesNotFoundAmount_returns_configured_value() throws IOException {
            String yaml = """
                    floatingParticles:
                      notFound:
                        amount: 7
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesNotFoundAmount()).isEqualTo(7);
        }

        @Test
        void particlesFoundColors_returns_configured_list() throws IOException {
            String yaml = """
                    floatingParticles:
                      found:
                        colors:
                          - "255,0,0"
                          - "0,255,0"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesFoundColors()).containsExactly("255,0,0", "0,255,0");
        }

        @Test
        void particlesNotFoundColors_returns_configured_list() throws IOException {
            String yaml = """
                    floatingParticles:
                      notFound:
                        colors:
                          - "128,128,128"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.particlesNotFoundColors()).containsExactly("128,128,128");
        }

        @Test
        void hologramParticlePlayerViewDistance_returns_configured_value() throws IOException {
            String yaml = """
                    internalTask:
                      hologramParticlePlayerViewDistance: 32
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hologramParticlePlayerViewDistance()).isEqualTo(32);
        }
    }

    // =========================================================================
    // 9. Spin and Hint
    // =========================================================================

    @Nested
    class SpinAndHint {

        @Test
        void spinEnabled_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.spinEnabled()).isTrue();
        }

        @Test
        void spinEnabled_returns_false_when_configured() throws IOException {
            String yaml = """
                    spin:
                      enabled: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.spinEnabled()).isFalse();
        }

        @Test
        void spinSpeed_defaults_to_1() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.spinSpeed()).isEqualTo(1);
        }

        @Test
        void spinSpeed_returns_configured_value() throws IOException {
            String yaml = """
                    spin:
                      speed: 3
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.spinSpeed()).isEqualTo(3);
        }

        @Test
        void spinLinked_defaults_to_true() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.spinLinked()).isTrue();
        }

        @Test
        void spinLinked_returns_false_when_configured() throws IOException {
            String yaml = """
                    spin:
                      linked: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.spinLinked()).isFalse();
        }

        @Test
        void hintDistanceBlocks_defaults_to_16() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hintDistanceBlocks()).isEqualTo(16);
        }

        @Test
        void hintDistanceBlocks_returns_configured_value() throws IOException {
            String yaml = """
                    hint:
                      distance: 32
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hintDistanceBlocks()).isEqualTo(32);
        }

        @Test
        void hintFrequency_defaults_to_5() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.hintFrequency()).isEqualTo(5);
        }

        @Test
        void hintFrequency_returns_configured_value() throws IOException {
            String yaml = """
                    hint:
                      frequency: 10
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hintFrequency()).isEqualTo(10);
        }

        @Test
        void hintSoundVolume_returns_configured_value() throws IOException {
            String yaml = """
                    hint:
                      sound:
                        volume: 5
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hintSoundVolume()).isEqualTo(5);
        }

        @Test
        void hintActionBarMessage_returns_configured_value() throws IOException {
            String yaml = """
                    hint:
                      actionBarMessage: "&cCustom message"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.hintActionBarMessage()).isEqualTo("&cCustom message");
        }
    }

    // =========================================================================
    // 10. reloadConfig
    // =========================================================================

    @Nested
    class Reload {

        @Test
        void reloadConfig_picks_up_modified_values() throws IOException {
            java.io.File file = writeConfig("language: en");
            var service = new ConfigService(file);
            assertThat(service.language()).isEqualTo("en");

            Files.writeString(file.toPath(), "language: de");
            service.reloadConfig();

            assertThat(service.language()).isEqualTo("de");
        }

        @Test
        void reloadConfig_picks_up_new_keys() throws IOException {
            java.io.File file = writeConfig("");
            var service = new ConfigService(file);
            assertThat(service.heads()).isEmpty();

            String yaml = """
                    heads:
                      - "new_texture"
                    """;
            Files.writeString(file.toPath(), yaml);
            service.reloadConfig();

            assertThat(service.heads()).containsExactly("new_texture");
        }

        @Test
        void reloadConfig_clears_removed_keys() throws IOException {
            String yaml = """
                    headClick:
                      sounds:
                        alreadyOwn: "ENTITY_VILLAGER_NO"
                    """;
            java.io.File file = writeConfig(yaml);
            var service = new ConfigService(file);
            assertThat(service.headClickAlreadyOwnSound()).isEqualTo("ENTITY_VILLAGER_NO");

            Files.writeString(file.toPath(), "");
            service.reloadConfig();

            assertThat(service.headClickAlreadyOwnSound()).isNull();
        }

        @Test
        void reloadConfig_picks_up_boolean_change() throws IOException {
            java.io.File file = writeConfig("metrics: true");
            var service = new ConfigService(file);
            assertThat(service.metricsEnabled()).isTrue();

            Files.writeString(file.toPath(), "metrics: false");
            service.reloadConfig();

            assertThat(service.metricsEnabled()).isFalse();
        }
    }

    // =========================================================================
    // 11. Progress bar
    // =========================================================================

    @Nested
    class ProgressBar {

        @Test
        void progressBarBars_defaults_to_100() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.progressBarBars()).isEqualTo(100);
        }

        @Test
        void progressBarBars_returns_configured_value() throws IOException {
            String yaml = """
                    progressBar:
                      totalBars: 50
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.progressBarBars()).isEqualTo(50);
        }

        @Test
        void progressBarSymbol_returns_configured_value() throws IOException {
            String yaml = """
                    progressBar:
                      symbol: "|"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.progressBarSymbol()).isEqualTo("|");
        }

        @Test
        void progressBarNotCompletedColor_returns_configured_value() throws IOException {
            String yaml = """
                    progressBar:
                      notCompletedColor: "&7"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.progressBarNotCompletedColor()).isEqualTo("&7");
        }

        @Test
        void progressBarCompletedColor_returns_configured_value() throws IOException {
            String yaml = """
                    progressBar:
                      completedColor: "&a"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.progressBarCompletedColor()).isEqualTo("&a");
        }
    }

    // =========================================================================
    // 12. Heads Theme
    // =========================================================================

    @Nested
    class HeadsTheme {

        @Test
        void headsThemeEnabled_returns_true_when_configured() throws IOException {
            String yaml = """
                    headsTheme:
                      enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headsThemeEnabled()).isTrue();
        }

        @Test
        void headsThemeSelected_returns_configured_value() throws IOException {
            String yaml = """
                    headsTheme:
                      selected: "christmas"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headsThemeSelected()).isEqualTo("christmas");
        }

        @Test
        void headsTheme_returns_configured_themes() throws IOException {
            String yaml = """
                    headsTheme:
                      theme:
                        christmas:
                          - "texture_xmas_1"
                          - "texture_xmas_2"
                        halloween:
                          - "texture_halloween_1"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            HashMap<String, List<String>> themes = service.headsTheme();
            assertThat(themes).hasSize(2);
            assertThat(themes.get("christmas")).containsExactly("texture_xmas_1", "texture_xmas_2");
            assertThat(themes.get("halloween")).containsExactly("texture_halloween_1");
        }

        @Test
        void headsTheme_returns_empty_when_no_theme_section() throws IOException {
            var service = new ConfigService(writeConfig(""));

            assertThat(service.headsTheme()).isEmpty();
        }

        @Test
        void headsTheme_returns_empty_when_theme_section_is_null() throws IOException {
            String yaml = """
                    headsTheme:
                      enabled: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.headsTheme()).isEmpty();
        }
    }

    // =========================================================================
    // 13. External Interactions
    // =========================================================================

    @Nested
    class ExternalInteractions {

        @Test
        void preventPistonExtension_returns_false_when_configured() throws IOException {
            String yaml = """
                    externalInteractions:
                      piston: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.preventPistonExtension()).isFalse();
        }

        @Test
        void preventLiquidFlow_returns_false_when_configured() throws IOException {
            String yaml = """
                    externalInteractions:
                      water: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.preventLiquidFlow()).isFalse();
        }

        @Test
        void preventExplosion_returns_false_when_configured() throws IOException {
            String yaml = """
                    externalInteractions:
                      explosion: false
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.preventExplosion()).isFalse();
        }
    }

    // =========================================================================
    // 14. Placeholders
    // =========================================================================

    @Nested
    class Placeholders {

        @Test
        void placeholdersLeaderboardPrefix_returns_configured_value() throws IOException {
            String yaml = """
                    placeholders:
                      leaderboard:
                        prefix: "&6#"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.placeholdersLeaderboardPrefix()).isEqualTo("&6#");
        }

        @Test
        void placeholdersLeaderboardSuffix_returns_configured_value() throws IOException {
            String yaml = """
                    placeholders:
                      leaderboard:
                        suffix: " heads"
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.placeholdersLeaderboardSuffix()).isEqualTo(" heads");
        }

        @Test
        void placeholdersLeaderboardUseNickname_returns_true_when_configured() throws IOException {
            String yaml = """
                    placeholders:
                      leaderboard:
                        nickname: true
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.placeholdersLeaderboardUseNickname()).isTrue();
        }
    }

    // =========================================================================
    // 15. Internal Task
    // =========================================================================

    @Nested
    class InternalTask {

        @Test
        void delayGlobalTask_returns_configured_value() throws IOException {
            String yaml = """
                    internalTask:
                      delay: 40
                    """;
            var service = new ConfigService(writeConfig(yaml));

            assertThat(service.delayGlobalTask()).isEqualTo(40);
        }
    }

    // =========================================================================
    // 16. getConfig accessor
    // =========================================================================

    @Nested
    class GetConfig {

        @Test
        void getConfig_returns_non_null_configuration() throws IOException {
            var service = new ConfigService(writeConfig("language: en"));

            assertThat(service.getConfig()).isNotNull();
            assertThat(service.getConfig().getString("language")).isEqualTo("en");
        }

        @Test
        void getConfig_reflects_state_after_reload() throws IOException {
            java.io.File file = writeConfig("language: en");
            var service = new ConfigService(file);

            Files.writeString(file.toPath(), "language: fr");
            service.reloadConfig();

            assertThat(service.getConfig().getString("language")).isEqualTo("fr");
        }
    }

    // =========================================================================
    // 17. GUI icons
    // =========================================================================

    @Nested
    class GuiIcons {

        @Test
        void guiBackIcon_default_returnsSpruceDoor() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var icon = service.guiBackIcon();
            assertThat(icon).isNotNull();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.SPRUCE_DOOR);
        }

        @Test
        void guiBackIcon_custom_returnsConfiguredMaterial() throws IOException {
            var service = new ConfigService(writeConfig("gui:\n  backIcon:\n    type: DIAMOND"));
            var icon = service.guiBackIcon();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.DIAMOND);
        }

        @Test
        void guiBorderIcon_default_returnsGrayStainedGlassPane() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var icon = service.guiBorderIcon();
            assertThat(icon).isNotNull();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        }

        @Test
        void guiPreviousIcon_default_returnsArrow() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var icon = service.guiPreviousIcon();
            assertThat(icon).isNotNull();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.ARROW);
        }

        @Test
        void guiNextIcon_default_returnsArrow() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var icon = service.guiNextIcon();
            assertThat(icon).isNotNull();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.ARROW);
        }

        @Test
        void guiCloseIcon_default_returnsBarrier() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var icon = service.guiCloseIcon();
            assertThat(icon).isNotNull();
            assertThat(icon.toItemStack().getType()).isEqualTo(org.bukkit.Material.BARRIER);
        }
    }

    // =========================================================================
    // 18. Hint sound type
    // =========================================================================

    @Nested
    class HintSoundType {

        @Test
        void hintSoundType_default_returnsAmethystBlockChime() throws IOException {
            var service = new ConfigService(writeConfig(""));
            var sound = service.hintSoundType();
            assertThat(sound).isEqualTo(com.cryptomorin.xseries.XSound.BLOCK_AMETHYST_BLOCK_CHIME);
        }

        @Test
        void hintSoundType_validSound_returnsThatSound() throws IOException {
            var service = new ConfigService(writeConfig("hint:\n  sound:\n    sound: ENTITY_EXPERIENCE_ORB_PICKUP"));
            var sound = service.hintSoundType();
            assertThat(sound).isEqualTo(com.cryptomorin.xseries.XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
        }

        @Test
        void hintSoundType_invalidSound_fallsBackToDefault() throws IOException {
            var service = new ConfigService(writeConfig("hint:\n  sound:\n    sound: TOTALLY_INVALID_SOUND"));
            var sound = service.hintSoundType();
            assertThat(sound).isEqualTo(com.cryptomorin.xseries.XSound.BLOCK_AMETHYST_BLOCK_CHIME);
        }
    }
}
