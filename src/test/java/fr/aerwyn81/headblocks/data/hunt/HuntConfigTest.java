package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class HuntConfigTest {

    private MockedStatic<ConfigService> configMock;

    @BeforeEach
    void setUp() {
        configMock = mockStatic(ConfigService.class);
    }

    @AfterEach
    void tearDown() {
        configMock.close();
    }

    // ---- HeadClick Messages ----

    @Test
    void getHeadClickMessages_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        List<String> override = List.of("Custom message");
        config.setHeadClickMessages(override);

        assertThat(config.getHeadClickMessages()).isEqualTo(override);
    }

    @Test
    void getHeadClickMessages_noOverride_fallsBackToConfigService() {
        List<String> fallback = List.of("Global message");
        configMock.when(ConfigService::getHeadClickMessages).thenReturn(fallback);

        HuntConfig config = new HuntConfig();

        assertThat(config.getHeadClickMessages()).isEqualTo(fallback);
    }

    // ---- HeadClick Title Enabled ----

    @Test
    void isHeadClickTitleEnabled_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setHeadClickTitleEnabled(true);

        assertThat(config.isHeadClickTitleEnabled()).isTrue();
    }

    @Test
    void isHeadClickTitleEnabled_noOverride_fallsBackToConfigService() {
        configMock.when(ConfigService::isHeadClickTitleEnabled).thenReturn(false);

        HuntConfig config = new HuntConfig();

        assertThat(config.isHeadClickTitleEnabled()).isFalse();
    }

    // ---- Holograms Enabled (derived from found + notFound) ----

    @Test
    void isHologramsEnabled_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setHologramsEnabled(true);

        assertThat(config.isHologramsEnabled()).isTrue();
    }

    @Test
    void isHologramsEnabled_noOverride_derivedFromFoundAndNotFound() {
        configMock.when(ConfigService::isHologramsFoundEnabled).thenReturn(false);
        configMock.when(ConfigService::isHologramsNotFoundEnabled).thenReturn(true);

        HuntConfig config = new HuntConfig();

        assertThat(config.isHologramsEnabled()).isTrue();
    }

    @Test
    void isHologramsEnabled_noOverride_bothFalse_returnsFalse() {
        configMock.when(ConfigService::isHologramsFoundEnabled).thenReturn(false);
        configMock.when(ConfigService::isHologramsNotFoundEnabled).thenReturn(false);

        HuntConfig config = new HuntConfig();

        assertThat(config.isHologramsEnabled()).isFalse();
    }

    // ---- Hints Enabled (fallback uses hintDistance > 0) ----

    @Test
    void isHintsEnabled_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setHintsEnabled(true);

        assertThat(config.isHintsEnabled()).isTrue();
    }

    @Test
    void isHintsEnabled_noOverride_fallbackUsesHintDistance() {
        configMock.when(ConfigService::getHintDistanceBlocks).thenReturn(10);

        HuntConfig config = new HuntConfig();

        assertThat(config.isHintsEnabled()).isTrue();
    }

    @Test
    void isHintsEnabled_noOverride_hintDistanceZero_returnsFalse() {
        configMock.when(ConfigService::getHintDistanceBlocks).thenReturn(0);

        HuntConfig config = new HuntConfig();

        assertThat(config.isHintsEnabled()).isFalse();
    }

    // ---- TieredRewards ----

    @Test
    void getTieredRewards_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        List<TieredReward> override = List.of(
                new TieredReward(1, List.of("msg"), List.of("cmd"), List.of(), 1, false));
        config.setTieredRewards(override);

        assertThat(config.getTieredRewards()).isEqualTo(override);
    }

    @Test
    void getTieredRewards_noOverride_fallsBackToConfigService() {
        List<TieredReward> fallback = List.of(
                new TieredReward(5, List.of("a"), List.of("b"), List.of("c"), 2, true));
        configMock.when(ConfigService::getTieredRewards).thenReturn(fallback);

        HuntConfig config = new HuntConfig();

        assertThat(config.getTieredRewards()).isEqualTo(fallback);
    }

    // ---- Spin Enabled ----

    @Test
    void isSpinEnabled_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setSpinEnabled(true);

        assertThat(config.isSpinEnabled()).isTrue();
    }

    @Test
    void isSpinEnabled_noOverride_fallsBackToConfigService() {
        configMock.when(ConfigService::isSpinEnabled).thenReturn(false);

        HuntConfig config = new HuntConfig();

        assertThat(config.isSpinEnabled()).isFalse();
    }

    // ---- Particles Found Type ----

    @Test
    void getParticlesFoundType_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setParticlesFoundType("FLAME");

        assertThat(config.getParticlesFoundType()).isEqualTo("FLAME");
    }

    @Test
    void getParticlesFoundType_noOverride_fallsBackToConfigService() {
        configMock.when(ConfigService::getParticlesFoundType).thenReturn("HEART");

        HuntConfig config = new HuntConfig();

        assertThat(config.getParticlesFoundType()).isEqualTo("HEART");
    }

    // ---- Sounds fallback ----

    @Test
    void getHeadClickSoundFound_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setHeadClickSoundFound("ENTITY_PLAYER_LEVELUP");

        assertThat(config.getHeadClickSoundFound()).isEqualTo("ENTITY_PLAYER_LEVELUP");
    }

    @Test
    void getHeadClickSoundFound_noOverride_fallsBackToConfigService() {
        configMock.when(ConfigService::getHeadClickNotOwnSound).thenReturn("BLOCK_NOTE_BLOCK_PLING");

        HuntConfig config = new HuntConfig();

        assertThat(config.getHeadClickSoundFound()).isEqualTo("BLOCK_NOTE_BLOCK_PLING");
    }

    @Test
    void getHeadClickSoundAlreadyOwn_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        config.setHeadClickSoundAlreadyOwn("ENTITY_VILLAGER_NO");

        assertThat(config.getHeadClickSoundAlreadyOwn()).isEqualTo("ENTITY_VILLAGER_NO");
    }

    @Test
    void getHeadClickSoundAlreadyOwn_noOverride_fallsBackToConfigService() {
        configMock.when(ConfigService::getHeadClickAlreadyOwnSound).thenReturn("BLOCK_ANVIL_LAND");

        HuntConfig config = new HuntConfig();

        assertThat(config.getHeadClickSoundAlreadyOwn()).isEqualTo("BLOCK_ANVIL_LAND");
    }

    // ---- Hologram lines fallback ----

    @Test
    void getHologramsFoundLines_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        ArrayList<String> lines = new ArrayList<>(List.of("Found!"));
        config.setHologramsFoundLines(lines);

        assertThat(config.getHologramsFoundLines()).containsExactly("Found!");
    }

    @Test
    void getHologramsFoundLines_noOverride_fallsBackToConfigService() {
        ArrayList<String> fallback = new ArrayList<>(List.of("Default found"));
        configMock.when(ConfigService::getHologramsFoundLines).thenReturn(fallback);

        HuntConfig config = new HuntConfig();

        assertThat(config.getHologramsFoundLines()).containsExactly("Default found");
    }

    @Test
    void getHologramsNotFoundLines_override_returnsOverride() {
        HuntConfig config = new HuntConfig();
        ArrayList<String> lines = new ArrayList<>(List.of("Not found!"));
        config.setHologramsNotFoundLines(lines);

        assertThat(config.getHologramsNotFoundLines()).containsExactly("Not found!");
    }

    @Test
    void getHologramsNotFoundLines_noOverride_fallsBackToConfigService() {
        ArrayList<String> fallback = new ArrayList<>(List.of("Default not found"));
        configMock.when(ConfigService::getHologramsNotFoundLines).thenReturn(fallback);

        HuntConfig config = new HuntConfig();

        assertThat(config.getHologramsNotFoundLines()).containsExactly("Default not found");
    }

    // ---- Raw check methods ----

    @Test
    void hasHeadClickMessages_true_whenSet() {
        HuntConfig config = new HuntConfig();
        config.setHeadClickMessages(List.of("msg"));

        assertThat(config.hasHeadClickMessages()).isTrue();
    }

    @Test
    void hasHeadClickMessages_false_whenNull() {
        HuntConfig config = new HuntConfig();

        assertThat(config.hasHeadClickMessages()).isFalse();
    }

    @Test
    void hasTieredRewards_true_whenSet() {
        HuntConfig config = new HuntConfig();
        config.setTieredRewards(List.of());

        assertThat(config.hasTieredRewards()).isTrue();
    }

    @Test
    void hasTieredRewards_false_whenNull() {
        HuntConfig config = new HuntConfig();

        assertThat(config.hasTieredRewards()).isFalse();
    }

    @Test
    void hasSpinConfig_true_whenSet() {
        HuntConfig config = new HuntConfig();
        config.setSpinEnabled(false);

        assertThat(config.hasSpinConfig()).isTrue();
    }

    @Test
    void hasSpinConfig_false_whenNull() {
        HuntConfig config = new HuntConfig();

        assertThat(config.hasSpinConfig()).isFalse();
    }

    @Test
    void hasParticlesConfig_true_whenFoundSet() {
        HuntConfig config = new HuntConfig();
        config.setParticlesFoundEnabled(true);

        assertThat(config.hasParticlesConfig()).isTrue();
    }

    @Test
    void hasHintsConfig_true_whenSet() {
        HuntConfig config = new HuntConfig();
        config.setHintsEnabled(true);

        assertThat(config.hasHintsConfig()).isTrue();
    }

    @Test
    void hasHintsConfig_false_whenNull() {
        HuntConfig config = new HuntConfig();

        assertThat(config.hasHintsConfig()).isFalse();
    }
}
