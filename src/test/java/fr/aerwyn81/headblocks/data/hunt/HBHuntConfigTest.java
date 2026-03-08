package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HBHuntConfigTest {

    @Mock
    ConfigService configService;

    private HuntConfig newConfig() {
        return new HuntConfig(configService);
    }

    // ---- HeadClick Messages ----

    @Test
    void getHeadClickMessages_override_returnsOverride() {
        HuntConfig config = newConfig();
        List<String> override = List.of("Custom message");
        config.setHeadClickMessages(override);

        assertThat(config.getHeadClickMessages()).isEqualTo(override);
    }

    @Test
    void getHeadClickMessages_noOverride_fallsBackToConfigService() {
        List<String> fallback = List.of("Global message");
        when(configService.headClickMessages()).thenReturn(fallback);

        HuntConfig config = newConfig();

        assertThat(config.getHeadClickMessages()).isEqualTo(fallback);
    }

    // ---- HeadClick Title Enabled ----

    @Test
    void isHeadClickTitleEnabled_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleEnabled(true);

        assertThat(config.isHeadClickTitleEnabled()).isTrue();
    }

    @Test
    void isHeadClickTitleEnabled_noOverride_fallsBackToConfigService() {
        when(configService.headClickTitleEnabled()).thenReturn(false);

        HuntConfig config = newConfig();

        assertThat(config.isHeadClickTitleEnabled()).isFalse();
    }

    // ---- Holograms Enabled (derived from found + notFound) ----

    @Test
    void isHologramsEnabled_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setHologramsEnabled(true);

        assertThat(config.isHologramsEnabled()).isTrue();
    }

    @Test
    void isHologramsEnabled_noOverride_derivedFromFoundAndNotFound() {
        when(configService.hologramsFoundEnabled()).thenReturn(false);
        when(configService.hologramsNotFoundEnabled()).thenReturn(true);

        HuntConfig config = newConfig();

        assertThat(config.isHologramsEnabled()).isTrue();
    }

    @Test
    void isHologramsEnabled_noOverride_bothFalse_returnsFalse() {
        when(configService.hologramsFoundEnabled()).thenReturn(false);
        when(configService.hologramsNotFoundEnabled()).thenReturn(false);

        HuntConfig config = newConfig();

        assertThat(config.isHologramsEnabled()).isFalse();
    }

    // ---- Hints Enabled (fallback uses hintDistance > 0) ----

    @Test
    void isHintsEnabled_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setHintsEnabled(true);

        assertThat(config.isHintsEnabled()).isTrue();
    }

    @Test
    void isHintsEnabled_noOverride_fallbackUsesHintDistance() {
        when(configService.hintDistanceBlocks()).thenReturn(10);

        HuntConfig config = newConfig();

        assertThat(config.isHintsEnabled()).isTrue();
    }

    @Test
    void isHintsEnabled_noOverride_hintDistanceZero_returnsFalse() {
        when(configService.hintDistanceBlocks()).thenReturn(0);

        HuntConfig config = newConfig();

        assertThat(config.isHintsEnabled()).isFalse();
    }

    // ---- TieredRewards ----

    @Test
    void getTieredRewards_override_returnsOverride() {
        HuntConfig config = newConfig();
        List<TieredReward> override = List.of(
                new TieredReward(1, List.of("msg"), List.of("cmd"), List.of(), 1, false));
        config.setTieredRewards(override);

        assertThat(config.getTieredRewards()).isEqualTo(override);
    }

    @Test
    void getTieredRewards_noOverride_fallsBackToConfigService() {
        List<TieredReward> fallback = List.of(
                new TieredReward(5, List.of("a"), List.of("b"), List.of("c"), 2, true));
        when(configService.tieredRewards()).thenReturn(fallback);

        HuntConfig config = newConfig();

        assertThat(config.getTieredRewards()).isEqualTo(fallback);
    }

    // ---- Spin Enabled ----

    @Test
    void isSpinEnabled_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setSpinEnabled(true);

        assertThat(config.isSpinEnabled()).isTrue();
    }

    @Test
    void isSpinEnabled_noOverride_fallsBackToConfigService() {
        when(configService.spinEnabled()).thenReturn(false);

        HuntConfig config = newConfig();

        assertThat(config.isSpinEnabled()).isFalse();
    }

    // ---- Particles Found Type ----

    @Test
    void getParticlesFoundType_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setParticlesFoundType("FLAME");

        assertThat(config.getParticlesFoundType()).isEqualTo("FLAME");
    }

    @Test
    void getParticlesFoundType_noOverride_fallsBackToConfigService() {
        when(configService.particlesFoundType()).thenReturn("HEART");

        HuntConfig config = newConfig();

        assertThat(config.getParticlesFoundType()).isEqualTo("HEART");
    }

    // ---- Sounds fallback ----

    @Test
    void getHeadClickSoundFound_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setHeadClickSoundFound("ENTITY_PLAYER_LEVELUP");

        assertThat(config.getHeadClickSoundFound()).isEqualTo("ENTITY_PLAYER_LEVELUP");
    }

    @Test
    void getHeadClickSoundFound_noOverride_fallsBackToConfigService() {
        when(configService.headClickNotOwnSound()).thenReturn("BLOCK_NOTE_BLOCK_PLING");

        HuntConfig config = newConfig();

        assertThat(config.getHeadClickSoundFound()).isEqualTo("BLOCK_NOTE_BLOCK_PLING");
    }

    @Test
    void getHeadClickSoundAlreadyOwn_override_returnsOverride() {
        HuntConfig config = newConfig();
        config.setHeadClickSoundAlreadyOwn("ENTITY_VILLAGER_NO");

        assertThat(config.getHeadClickSoundAlreadyOwn()).isEqualTo("ENTITY_VILLAGER_NO");
    }

    @Test
    void getHeadClickSoundAlreadyOwn_noOverride_fallsBackToConfigService() {
        when(configService.headClickAlreadyOwnSound()).thenReturn("BLOCK_ANVIL_LAND");

        HuntConfig config = newConfig();

        assertThat(config.getHeadClickSoundAlreadyOwn()).isEqualTo("BLOCK_ANVIL_LAND");
    }

    // ---- Hologram lines fallback ----

    @Test
    void getHologramsFoundLines_override_returnsOverride() {
        HuntConfig config = newConfig();
        ArrayList<String> lines = new ArrayList<>(List.of("Found!"));
        config.setHologramsFoundLines(lines);

        assertThat(config.getHologramsFoundLines()).containsExactly("Found!");
    }

    @Test
    void getHologramsFoundLines_noOverride_fallsBackToConfigService() {
        ArrayList<String> fallback = new ArrayList<>(List.of("Default found"));
        when(configService.hologramsFoundLines()).thenReturn(fallback);

        HuntConfig config = newConfig();

        assertThat(config.getHologramsFoundLines()).containsExactly("Default found");
    }

    @Test
    void getHologramsNotFoundLines_override_returnsOverride() {
        HuntConfig config = newConfig();
        ArrayList<String> lines = new ArrayList<>(List.of("Not found!"));
        config.setHologramsNotFoundLines(lines);

        assertThat(config.getHologramsNotFoundLines()).containsExactly("Not found!");
    }

    @Test
    void getHologramsNotFoundLines_noOverride_fallsBackToConfigService() {
        ArrayList<String> fallback = new ArrayList<>(List.of("Default not found"));
        when(configService.hologramsNotFoundLines()).thenReturn(fallback);

        HuntConfig config = newConfig();

        assertThat(config.getHologramsNotFoundLines()).containsExactly("Default not found");
    }

    // ---- Raw check methods ----

    @Test
    void hasHeadClickMessages_true_whenSet() {
        HuntConfig config = newConfig();
        config.setHeadClickMessages(List.of("msg"));

        assertThat(config.hasHeadClickMessages()).isTrue();
    }

    @Test
    void hasHeadClickMessages_false_whenNull() {
        HuntConfig config = newConfig();

        assertThat(config.hasHeadClickMessages()).isFalse();
    }

    @Test
    void hasTieredRewards_true_whenSet() {
        HuntConfig config = newConfig();
        config.setTieredRewards(List.of());

        assertThat(config.hasTieredRewards()).isTrue();
    }

    @Test
    void hasTieredRewards_false_whenNull() {
        HuntConfig config = newConfig();

        assertThat(config.hasTieredRewards()).isFalse();
    }

    @Test
    void hasSpinConfig_true_whenSet() {
        HuntConfig config = newConfig();
        config.setSpinEnabled(false);

        assertThat(config.hasSpinConfig()).isTrue();
    }

    @Test
    void hasSpinConfig_false_whenNull() {
        HuntConfig config = newConfig();

        assertThat(config.hasSpinConfig()).isFalse();
    }

    @Test
    void hasParticlesConfig_true_whenFoundSet() {
        HuntConfig config = newConfig();
        config.setParticlesFoundEnabled(true);

        assertThat(config.hasParticlesConfig()).isTrue();
    }

    @Test
    void hasHintsConfig_true_whenSet() {
        HuntConfig config = newConfig();
        config.setHintsEnabled(true);

        assertThat(config.hasHintsConfig()).isTrue();
    }

    @Test
    void hasHintsConfig_false_whenNull() {
        HuntConfig config = newConfig();

        assertThat(config.hasHintsConfig()).isFalse();
    }

    // ---- Title strings ----

    @Test
    void getHeadClickTitleFirstLine_override() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleFirstLine("Custom Title");

        assertThat(config.getHeadClickTitleFirstLine()).isEqualTo("Custom Title");
    }

    @Test
    void getHeadClickTitleFirstLine_fallback() {
        when(configService.headClickTitleFirstLine()).thenReturn("Default Title");

        assertThat(newConfig().getHeadClickTitleFirstLine()).isEqualTo("Default Title");
    }

    @Test
    void getHeadClickTitleSubTitle_override() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleSubTitle("Custom Sub");

        assertThat(config.getHeadClickTitleSubTitle()).isEqualTo("Custom Sub");
    }

    @Test
    void getHeadClickTitleSubTitle_fallback() {
        when(configService.headClickTitleSubTitle()).thenReturn("Default Sub");

        assertThat(newConfig().getHeadClickTitleSubTitle()).isEqualTo("Default Sub");
    }

    // ---- Title timing ----

    @Test
    void getHeadClickTitleFadeIn_override() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleFadeIn(10);

        assertThat(config.getHeadClickTitleFadeIn()).isEqualTo(10);
    }

    @Test
    void getHeadClickTitleFadeIn_fallback() {
        when(configService.headClickTitleFadeIn()).thenReturn(5);

        assertThat(newConfig().getHeadClickTitleFadeIn()).isEqualTo(5);
    }

    @Test
    void getHeadClickTitleStay_override() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleStay(20);

        assertThat(config.getHeadClickTitleStay()).isEqualTo(20);
    }

    @Test
    void getHeadClickTitleStay_fallback() {
        when(configService.headClickTitleStay()).thenReturn(15);

        assertThat(newConfig().getHeadClickTitleStay()).isEqualTo(15);
    }

    @Test
    void getHeadClickTitleFadeOut_override() {
        HuntConfig config = newConfig();
        config.setHeadClickTitleFadeOut(8);

        assertThat(config.getHeadClickTitleFadeOut()).isEqualTo(8);
    }

    @Test
    void getHeadClickTitleFadeOut_fallback() {
        when(configService.headClickTitleFadeOut()).thenReturn(3);

        assertThat(newConfig().getHeadClickTitleFadeOut()).isEqualTo(3);
    }

    // ---- Firework ----

    @Test
    void isFireworkEnabled_override() {
        HuntConfig config = newConfig();
        config.setFireworkEnabled(true);

        assertThat(config.isFireworkEnabled()).isTrue();
    }

    @Test
    void isFireworkEnabled_fallback() {
        when(configService.fireworkEnabled()).thenReturn(false);

        assertThat(newConfig().isFireworkEnabled()).isFalse();
    }

    // ---- HeadClick commands ----

    @Test
    void getHeadClickCommands_override() {
        HuntConfig config = newConfig();
        config.setHeadClickCommands(List.of("cmd1", "cmd2"));

        assertThat(config.getHeadClickCommands()).containsExactly("cmd1", "cmd2");
    }

    @Test
    void getHeadClickCommands_fallback() {
        when(configService.headClickCommands()).thenReturn(List.of("globalCmd"));

        assertThat(newConfig().getHeadClickCommands()).containsExactly("globalCmd");
    }

    // ---- Eject ----

    @Test
    void isHeadClickEjectEnabled_override() {
        HuntConfig config = newConfig();
        config.setHeadClickEjectEnabled(true);

        assertThat(config.isHeadClickEjectEnabled()).isTrue();
    }

    @Test
    void isHeadClickEjectEnabled_fallback() {
        when(configService.headClickEjectEnabled()).thenReturn(false);

        assertThat(newConfig().isHeadClickEjectEnabled()).isFalse();
    }

    @Test
    void getHeadClickEjectPower_override() {
        HuntConfig config = newConfig();
        config.setHeadClickEjectPower(2.5);

        assertThat(config.getHeadClickEjectPower()).isEqualTo(2.5);
    }

    @Test
    void getHeadClickEjectPower_fallback() {
        when(configService.headClickEjectPower()).thenReturn(1.0);

        assertThat(newConfig().getHeadClickEjectPower()).isEqualTo(1.0);
    }

    // ---- Holograms found/notFound individual ----

    @Test
    void isHologramsFoundEnabled_override() {
        HuntConfig config = newConfig();
        config.setHologramsFoundEnabled(true);

        assertThat(config.isHologramsFoundEnabled()).isTrue();
    }

    @Test
    void isHologramsFoundEnabled_fallback() {
        when(configService.hologramsFoundEnabled()).thenReturn(false);

        assertThat(newConfig().isHologramsFoundEnabled()).isFalse();
    }

    @Test
    void isHologramsNotFoundEnabled_override() {
        HuntConfig config = newConfig();
        config.setHologramsNotFoundEnabled(true);

        assertThat(config.isHologramsNotFoundEnabled()).isTrue();
    }

    @Test
    void isHologramsNotFoundEnabled_fallback() {
        when(configService.hologramsNotFoundEnabled()).thenReturn(false);

        assertThat(newConfig().isHologramsNotFoundEnabled()).isFalse();
    }

    // ---- Hint distance/frequency ----

    @Test
    void getHintDistance_override() {
        HuntConfig config = newConfig();
        config.setHintDistance(50);

        assertThat(config.getHintDistance()).isEqualTo(50);
    }

    @Test
    void getHintDistance_fallback() {
        when(configService.hintDistanceBlocks()).thenReturn(30);

        assertThat(newConfig().getHintDistance()).isEqualTo(30);
    }

    @Test
    void getHintFrequency_override() {
        HuntConfig config = newConfig();
        config.setHintFrequency(5);

        assertThat(config.getHintFrequency()).isEqualTo(5);
    }

    @Test
    void getHintFrequency_fallback() {
        when(configService.hintFrequency()).thenReturn(10);

        assertThat(newConfig().getHintFrequency()).isEqualTo(10);
    }

    // ---- Spin speed/linked ----

    @Test
    void getSpinSpeed_override() {
        HuntConfig config = newConfig();
        config.setSpinSpeed(3);

        assertThat(config.getSpinSpeed()).isEqualTo(3);
    }

    @Test
    void getSpinSpeed_fallback() {
        when(configService.spinSpeed()).thenReturn(2);

        assertThat(newConfig().getSpinSpeed()).isEqualTo(2);
    }

    @Test
    void isSpinLinked_override() {
        HuntConfig config = newConfig();
        config.setSpinLinked(true);

        assertThat(config.isSpinLinked()).isTrue();
    }

    @Test
    void isSpinLinked_fallback() {
        when(configService.spinLinked()).thenReturn(false);

        assertThat(newConfig().isSpinLinked()).isFalse();
    }

    // ---- Particles remaining ----

    @Test
    void isParticlesFoundEnabled_override() {
        HuntConfig config = newConfig();
        config.setParticlesFoundEnabled(true);

        assertThat(config.isParticlesFoundEnabled()).isTrue();
    }

    @Test
    void isParticlesFoundEnabled_fallback() {
        when(configService.particlesFoundEnabled()).thenReturn(false);

        assertThat(newConfig().isParticlesFoundEnabled()).isFalse();
    }

    @Test
    void isParticlesNotFoundEnabled_override() {
        HuntConfig config = newConfig();
        config.setParticlesNotFoundEnabled(true);

        assertThat(config.isParticlesNotFoundEnabled()).isTrue();
    }

    @Test
    void isParticlesNotFoundEnabled_fallback() {
        when(configService.particlesNotFoundEnabled()).thenReturn(false);

        assertThat(newConfig().isParticlesNotFoundEnabled()).isFalse();
    }

    @Test
    void getParticlesNotFoundType_override() {
        HuntConfig config = newConfig();
        config.setParticlesNotFoundType("SMOKE");

        assertThat(config.getParticlesNotFoundType()).isEqualTo("SMOKE");
    }

    @Test
    void getParticlesNotFoundType_fallback() {
        when(configService.particlesNotFoundType()).thenReturn("VILLAGER_HAPPY");

        assertThat(newConfig().getParticlesNotFoundType()).isEqualTo("VILLAGER_HAPPY");
    }

    @Test
    void getParticlesNotFoundAmount_override() {
        HuntConfig config = newConfig();
        config.setParticlesNotFoundAmount(5);

        assertThat(config.getParticlesNotFoundAmount()).isEqualTo(5);
    }

    @Test
    void getParticlesNotFoundAmount_fallback() {
        when(configService.particlesNotFoundAmount()).thenReturn(3);

        assertThat(newConfig().getParticlesNotFoundAmount()).isEqualTo(3);
    }

    @Test
    void getParticlesFoundAmount_override() {
        HuntConfig config = newConfig();
        config.setParticlesFoundAmount(10);

        assertThat(config.getParticlesFoundAmount()).isEqualTo(10);
    }

    @Test
    void getParticlesFoundAmount_fallback() {
        when(configService.particlesFoundAmount()).thenReturn(7);

        assertThat(newConfig().getParticlesFoundAmount()).isEqualTo(7);
    }

    // ---- hasParticlesConfig with NotFound ----

    @Test
    void hasParticlesConfig_true_whenNotFoundSet() {
        HuntConfig config = newConfig();
        config.setParticlesNotFoundEnabled(true);

        assertThat(config.hasParticlesConfig()).isTrue();
    }

    @Test
    void hasParticlesConfig_false_whenNoneSet() {
        HuntConfig config = newConfig();

        assertThat(config.hasParticlesConfig()).isFalse();
    }

    // ---- hasHologramsFoundLines / hasHologramsNotFoundLines ----

    @Test
    void hasHologramsFoundLines_true_whenSet() {
        HuntConfig config = newConfig();
        config.setHologramsFoundLines(new ArrayList<>(List.of("line")));

        assertThat(config.hasHologramsFoundLines()).isTrue();
    }

    @Test
    void hasHologramsFoundLines_false_whenNull() {
        assertThat(newConfig().hasHologramsFoundLines()).isFalse();
    }

    @Test
    void hasHologramsNotFoundLines_true_whenSet() {
        HuntConfig config = newConfig();
        config.setHologramsNotFoundLines(new ArrayList<>(List.of("line")));

        assertThat(config.hasHologramsNotFoundLines()).isTrue();
    }

    @Test
    void hasHologramsNotFoundLines_false_whenNull() {
        assertThat(newConfig().hasHologramsNotFoundLines()).isFalse();
    }
}
