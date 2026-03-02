package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HuntConfigServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private ConfigService configService;

    @Mock
    private ServiceRegistry registry;

    private HuntConfigService huntConfigService;

    @BeforeEach
    void setUp() {
        lenient().when(pluginProvider.getDataFolder()).thenReturn(tempDir.toFile());

        // Mock all ConfigService methods used in generateDefaultFromConfig()
        lenient().when(configService.headClickMessages()).thenReturn(new ArrayList<>());
        lenient().when(configService.headClickTitleEnabled()).thenReturn(false);
        lenient().when(configService.headClickTitleFirstLine()).thenReturn("");
        lenient().when(configService.headClickTitleSubTitle()).thenReturn("");
        lenient().when(configService.headClickTitleFadeIn()).thenReturn(0);
        lenient().when(configService.headClickTitleStay()).thenReturn(50);
        lenient().when(configService.headClickTitleFadeOut()).thenReturn(0);
        lenient().when(configService.headClickNotOwnSound()).thenReturn(null);
        lenient().when(configService.headClickAlreadyOwnSound()).thenReturn(null);
        lenient().when(configService.fireworkEnabled()).thenReturn(false);
        lenient().when(configService.headClickCommands()).thenReturn(new ArrayList<>());
        lenient().when(configService.headClickEjectEnabled()).thenReturn(false);
        lenient().when(configService.headClickEjectPower()).thenReturn(1.0);
        lenient().when(configService.hologramsFoundEnabled()).thenReturn(true);
        lenient().when(configService.hologramsNotFoundEnabled()).thenReturn(true);
        lenient().when(configService.hologramsFoundLines()).thenReturn(new ArrayList<>());
        lenient().when(configService.hologramsNotFoundLines()).thenReturn(new ArrayList<>());
        lenient().when(configService.hintDistanceBlocks()).thenReturn(16);
        lenient().when(configService.hintFrequency()).thenReturn(5);
        lenient().when(configService.spinEnabled()).thenReturn(true);
        lenient().when(configService.spinSpeed()).thenReturn(1);
        lenient().when(configService.spinLinked()).thenReturn(true);
        lenient().when(configService.particlesFoundEnabled()).thenReturn(true);
        lenient().when(configService.particlesFoundType()).thenReturn("REDSTONE");
        lenient().when(configService.particlesFoundAmount()).thenReturn(3);
        lenient().when(configService.particlesNotFoundEnabled()).thenReturn(false);
        lenient().when(configService.particlesNotFoundType()).thenReturn("REDSTONE");
        lenient().when(configService.particlesNotFoundAmount()).thenReturn(3);
        lenient().when(configService.tieredRewards()).thenReturn(new ArrayList<>());

        huntConfigService = new HuntConfigService(pluginProvider, configService, registry);
    }

    // --- Constructor / initialize ---

    @Test
    void constructor_createsHuntsDirectory() {
        File huntsDir = new File(tempDir.toFile(), "hunts");

        assertThat(huntsDir).exists();
        assertThat(huntsDir).isDirectory();
    }

    @Test
    void constructor_generatesDefaultYmlIfNotExists() {
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");

        assertThat(defaultFile).exists();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(defaultFile);
        assertThat(yaml.getString("id")).isEqualTo("default");
        assertThat(yaml.getString("displayName")).isEqualTo("Default");
        assertThat(yaml.getString("state")).isEqualTo("ACTIVE");
        assertThat(yaml.getInt("priority")).isEqualTo(0);
        assertThat(yaml.getString("icon")).isEqualTo("CHEST_MINECART");
    }

    @Test
    void constructor_doesNotOverwriteExistingDefaultYml() throws IOException {
        // The setUp() already created default.yml via the constructor.
        // Modify its content to confirm it is not regenerated on re-initialize.
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(defaultFile);
        yaml.set("displayName", "CustomDefault");
        yaml.save(defaultFile);

        // Re-initialize
        huntConfigService.initialize();

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(defaultFile);
        assertThat(reloaded.getString("displayName")).isEqualTo("CustomDefault");
    }

    // --- loadHunts ---

    @Test
    void loadHunts_emptyDirectory_returnsEmptyList() throws IOException {
        // Delete the generated default.yml so hunts dir is empty
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");
        assertThat(defaultFile.delete()).isTrue();

        List<Hunt> hunts = huntConfigService.loadHunts();

        assertThat(hunts).isEmpty();
    }

    @Test
    void loadHunts_withValidYamlFiles_returnsHunts() throws IOException {
        // default.yml already exists from constructor, add a second
        File huntsDir = new File(tempDir.toFile(), "hunts");
        File secondFile = new File(huntsDir, "second.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "second");
        yaml.set("displayName", "Second Hunt");
        yaml.set("state", "ACTIVE");
        yaml.set("priority", 5);
        yaml.set("icon", "DIAMOND");
        yaml.save(secondFile);

        List<Hunt> hunts = huntConfigService.loadHunts();

        assertThat(hunts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(hunts).extracting(Hunt::getId).contains("default", "second");
    }

    @Test
    void loadHunts_skipsNonYmlFiles() throws IOException {
        File huntsDir = new File(tempDir.toFile(), "hunts");
        File txtFile = new File(huntsDir, "notes.txt");
        assertThat(txtFile.createNewFile()).isTrue();

        List<Hunt> hunts = huntConfigService.loadHunts();

        // Should only contain default.yml, not notes.txt
        assertThat(hunts).extracting(Hunt::getId).doesNotContain("notes");
    }

    @Test
    void loadHunts_skipsFilesWithMissingId() throws IOException {
        File huntsDir = new File(tempDir.toFile(), "hunts");
        File badFile = new File(huntsDir, "noid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("displayName", "No ID Hunt");
        yaml.save(badFile);

        List<Hunt> hunts = huntConfigService.loadHunts();

        // noid.yml should be skipped; default.yml should still load
        assertThat(hunts).extracting(Hunt::getId).doesNotContain((String) null);
        assertThat(hunts).extracting(Hunt::getId).contains("default");
    }

    // --- loadHunt ---

    @Test
    void loadHunt_missingIdField_returnsNull() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/noid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("displayName", "No ID");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNull();
    }

    @Test
    void loadHunt_emptyIdField_returnsNull() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/emptyid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "");
        yaml.set("displayName", "Empty ID");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNull();
    }

    @Test
    void loadHunt_validData_returnsHunt() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/valid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "myHunt");
        yaml.set("displayName", "My Hunt");
        yaml.set("state", "INACTIVE");
        yaml.set("priority", 10);
        yaml.set("icon", "GOLD_BLOCK");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("myHunt");
        assertThat(result.getDisplayName()).isEqualTo("My Hunt");
        assertThat(result.getState()).isEqualTo(HuntState.INACTIVE);
        assertThat(result.getPriority()).isEqualTo(10);
        assertThat(result.getIcon()).isEqualTo("GOLD_BLOCK");
    }

    @Test
    void loadHunt_usesDefaults_whenOptionalFieldsMissing() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/minimal.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "minimal");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("minimal");
        assertThat(result.getDisplayName()).isEqualTo("minimal"); // defaults to id
        assertThat(result.getState()).isEqualTo(HuntState.ACTIVE); // default
        assertThat(result.getPriority()).isEqualTo(1); // default
        assertThat(result.getIcon()).isEqualTo("CHEST_MINECART"); // default
    }

    @Test
    void loadHunt_loadsBehaviors_freeBehaviorFromSection() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/withbehavior.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "bhunt");
        yaml.createSection("behaviors.free");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        assertThat(result.getBehaviors()).isNotEmpty();
        assertThat(result.getBehaviors().get(0).getId()).isEqualTo("free");
    }

    @Test
    void loadHunt_noBehaviorsSection_usesDefaultFreeBehavior() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/nobehavior.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "nobhunt");
        yaml.save(file);

        Hunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        // setBehaviors with empty list should fall back to FreeBehavior
        assertThat(result.getBehaviors()).isNotEmpty();
        assertThat(result.getBehaviors().get(0).getId()).isEqualTo("free");
    }

    // --- saveHunt and round-trip ---

    @Test
    void saveHunt_createsFileOnDisk() {
        Hunt hunt = new Hunt(configService, "saveme", "Save Me", HuntState.ACTIVE, 3, "EMERALD");

        huntConfigService.saveHunt(hunt);

        File savedFile = new File(tempDir.toFile(), "hunts/saveme.yml");
        assertThat(savedFile).exists();
    }

    @Test
    void saveHunt_writesCorrectValues() {
        Hunt hunt = new Hunt(configService, "test1", "Test One", HuntState.ARCHIVED, 7, "BEACON");

        huntConfigService.saveHunt(hunt);

        File savedFile = new File(tempDir.toFile(), "hunts/test1.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(savedFile);

        assertThat(yaml.getString("id")).isEqualTo("test1");
        assertThat(yaml.getString("displayName")).isEqualTo("Test One");
        assertThat(yaml.getString("state")).isEqualTo("ARCHIVED");
        assertThat(yaml.getInt("priority")).isEqualTo(7);
        assertThat(yaml.getString("icon")).isEqualTo("BEACON");
    }

    @Test
    void saveAndLoad_roundTrip_preservesCoreFields() {
        Hunt original = new Hunt(configService, "roundtrip", "Round Trip", HuntState.INACTIVE, 42, "HOPPER");

        huntConfigService.saveHunt(original);

        File file = new File(tempDir.toFile(), "hunts/roundtrip.yml");
        Hunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo("roundtrip");
        assertThat(loaded.getDisplayName()).isEqualTo("Round Trip");
        assertThat(loaded.getState()).isEqualTo(HuntState.INACTIVE);
        assertThat(loaded.getPriority()).isEqualTo(42);
        assertThat(loaded.getIcon()).isEqualTo("HOPPER");
    }

    @Test
    void saveAndLoad_roundTrip_preservesHuntConfigMessages() {
        Hunt hunt = new Hunt(configService, "msgtrip", "Msg Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        hc.setHeadClickMessages(List.of("Hello!", "World!"));

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/msgtrip.yml");
        Hunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHeadClickMessages()).containsExactly("Hello!", "World!");
    }

    @Test
    void saveAndLoad_roundTrip_preservesHologramLines() {
        Hunt hunt = new Hunt(configService, "holotrip", "Holo Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        hc.setHologramsFoundLines(new ArrayList<>(List.of("&aFound!")));
        hc.setHologramsNotFoundLines(new ArrayList<>(List.of("&cNot found!")));

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/holotrip.yml");
        Hunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHologramsFoundLines()).containsExactly("&aFound!");
        assertThat(loaded.getConfig().getHologramsNotFoundLines()).containsExactly("&cNot found!");
    }

    @Test
    void saveAndLoad_roundTrip_preservesTieredRewards() {
        Hunt hunt = new Hunt(configService, "tiertrip", "Tier Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        List<TieredReward> rewards = List.of(
                new TieredReward(5, List.of("Level 5!"), List.of("give %player% diamond 1"), List.of("Broadcast!"), 3, true),
                new TieredReward(10, List.of("Level 10!"), List.of("give %player% gold_ingot 5"), List.of(), -1, false)
        );
        hc.setTieredRewards(rewards);

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/tiertrip.yml");
        Hunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        List<TieredReward> loadedRewards = loaded.getConfig().getTieredRewards();
        assertThat(loadedRewards).hasSize(2);

        TieredReward r5 = loadedRewards.stream().filter(r -> r.level() == 5).findFirst().orElse(null);
        assertThat(r5).isNotNull();
        assertThat(r5.messages()).containsExactly("Level 5!");
        assertThat(r5.commands()).containsExactly("give %player% diamond 1");
        assertThat(r5.broadcastMessages()).containsExactly("Broadcast!");
        assertThat(r5.slotsRequired()).isEqualTo(3);
        assertThat(r5.isRandom()).isTrue();

        TieredReward r10 = loadedRewards.stream().filter(r -> r.level() == 10).findFirst().orElse(null);
        assertThat(r10).isNotNull();
        assertThat(r10.messages()).containsExactly("Level 10!");
        assertThat(r10.commands()).containsExactly("give %player% gold_ingot 5");
        assertThat(r10.slotsRequired()).isEqualTo(-1);
        assertThat(r10.isRandom()).isFalse();
    }

    // --- huntFileExists ---

    @Test
    void huntFileExists_existingFile_returnsTrue() {
        // default.yml was created by the constructor
        assertThat(huntConfigService.huntFileExists("default")).isTrue();
    }

    @Test
    void huntFileExists_nonExistentFile_returnsFalse() {
        assertThat(huntConfigService.huntFileExists("nonexistent")).isFalse();
    }

    @Test
    void huntFileExists_afterSave_returnsTrue() {
        Hunt hunt = new Hunt(configService, "newbie", "Newbie", HuntState.ACTIVE, 1, "CHEST");
        huntConfigService.saveHunt(hunt);

        assertThat(huntConfigService.huntFileExists("newbie")).isTrue();
    }

    // --- deleteHuntFile ---

    @Test
    void deleteHuntFile_existingFile_removesIt() {
        assertThat(huntConfigService.huntFileExists("default")).isTrue();

        huntConfigService.deleteHuntFile("default");

        assertThat(huntConfigService.huntFileExists("default")).isFalse();
    }

    @Test
    void deleteHuntFile_nonExistentFile_noError() {
        huntConfigService.deleteHuntFile("nonexistent"); // should not throw
        assertThat(huntConfigService.huntFileExists("nonexistent")).isFalse();
    }

    @Test
    void deleteHuntFile_afterSaveAndDelete_fileGone() {
        Hunt hunt = new Hunt(configService, "deleteme", "Delete Me", HuntState.ACTIVE, 1, "CHEST");
        huntConfigService.saveHunt(hunt);
        assertThat(huntConfigService.huntFileExists("deleteme")).isTrue();

        huntConfigService.deleteHuntFile("deleteme");

        assertThat(huntConfigService.huntFileExists("deleteme")).isFalse();
    }

    // --- loadHuntConfig (via loadHunt) ---

    @Test
    void loadHuntConfig_readsHeadClickConfigValues() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/hcconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "hcconfig");

        String p = "config.";
        yaml.set(p + "headClick.messages", List.of("Msg1", "Msg2"));
        yaml.set(p + "headClick.title.enabled", true);
        yaml.set(p + "headClick.title.firstLine", "&aTitle");
        yaml.set(p + "headClick.title.subTitle", "&bSubtitle");
        yaml.set(p + "headClick.title.fadeIn", 10);
        yaml.set(p + "headClick.title.stay", 70);
        yaml.set(p + "headClick.title.fadeOut", 20);
        yaml.set(p + "headClick.sound.found", "ENTITY_PLAYER_LEVELUP");
        yaml.set(p + "headClick.sound.alreadyOwn", "ENTITY_VILLAGER_NO");
        yaml.set(p + "headClick.firework.enabled", true);
        yaml.set(p + "headClick.commands", List.of("say found!", "give %player% diamond 1"));
        yaml.set(p + "headClick.eject.enabled", true);
        yaml.set(p + "headClick.eject.power", 2.5);
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        assertThat(hc.getHeadClickMessages()).containsExactly("Msg1", "Msg2");
        assertThat(hc.isHeadClickTitleEnabled()).isTrue();
        assertThat(hc.getHeadClickTitleFirstLine()).isEqualTo("&aTitle");
        assertThat(hc.getHeadClickTitleSubTitle()).isEqualTo("&bSubtitle");
        assertThat(hc.getHeadClickTitleFadeIn()).isEqualTo(10);
        assertThat(hc.getHeadClickTitleStay()).isEqualTo(70);
        assertThat(hc.getHeadClickTitleFadeOut()).isEqualTo(20);
        assertThat(hc.getHeadClickSoundFound()).isEqualTo("ENTITY_PLAYER_LEVELUP");
        assertThat(hc.getHeadClickSoundAlreadyOwn()).isEqualTo("ENTITY_VILLAGER_NO");
        assertThat(hc.isFireworkEnabled()).isTrue();
        assertThat(hc.getHeadClickCommands()).containsExactly("say found!", "give %player% diamond 1");
        assertThat(hc.isHeadClickEjectEnabled()).isTrue();
        assertThat(hc.getHeadClickEjectPower()).isEqualTo(2.5);
    }

    @Test
    void loadHuntConfig_readsHologramConfigValues() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/holoconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "holoconfig");

        String p = "config.";
        yaml.set(p + "holograms.enabled", false);
        yaml.set(p + "holograms.found.enabled", true);
        yaml.set(p + "holograms.notFound.enabled", false);
        yaml.set(p + "holograms.found.lines", List.of("&aFound it!", "&7Good job"));
        yaml.set(p + "holograms.notFound.lines", List.of("&cStill searching..."));
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        assertThat(hc.isHologramsEnabled()).isFalse();
        assertThat(hc.isHologramsFoundEnabled()).isTrue();
        assertThat(hc.isHologramsNotFoundEnabled()).isFalse();
        assertThat(hc.getHologramsFoundLines()).containsExactly("&aFound it!", "&7Good job");
        assertThat(hc.getHologramsNotFoundLines()).containsExactly("&cStill searching...");
    }

    @Test
    void loadHuntConfig_readsHintsConfigValues() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/hintsconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "hintsconfig");

        String p = "config.";
        yaml.set(p + "hints.enabled", true);
        yaml.set(p + "hints.distance", 32);
        yaml.set(p + "hints.frequency", 10);
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        assertThat(hc.isHintsEnabled()).isTrue();
        assertThat(hc.getHintDistance()).isEqualTo(32);
        assertThat(hc.getHintFrequency()).isEqualTo(10);
    }

    @Test
    void loadHuntConfig_readsSpinConfigValues() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/spinconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "spinconfig");

        String p = "config.";
        yaml.set(p + "spin.enabled", false);
        yaml.set(p + "spin.speed", 3);
        yaml.set(p + "spin.linked", false);
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        assertThat(hc.isSpinEnabled()).isFalse();
        assertThat(hc.getSpinSpeed()).isEqualTo(3);
        assertThat(hc.isSpinLinked()).isFalse();
    }

    @Test
    void loadHuntConfig_readsParticlesConfigValues() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/partconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "partconfig");

        String p = "config.";
        yaml.set(p + "particles.found.enabled", true);
        yaml.set(p + "particles.found.type", "FLAME");
        yaml.set(p + "particles.found.amount", 5);
        yaml.set(p + "particles.notFound.enabled", true);
        yaml.set(p + "particles.notFound.type", "HEART");
        yaml.set(p + "particles.notFound.amount", 2);
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        assertThat(hc.isParticlesFoundEnabled()).isTrue();
        assertThat(hc.getParticlesFoundType()).isEqualTo("FLAME");
        assertThat(hc.getParticlesFoundAmount()).isEqualTo(5);
        assertThat(hc.isParticlesNotFoundEnabled()).isTrue();
        assertThat(hc.getParticlesNotFoundType()).isEqualTo("HEART");
        assertThat(hc.getParticlesNotFoundAmount()).isEqualTo(2);
    }

    @Test
    void loadHuntConfig_readsTieredRewards() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/tierconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "tierconfig");

        String p = "config.";
        yaml.set(p + "tieredRewards.3.messages", List.of("3 found!"));
        yaml.set(p + "tieredRewards.3.commands", List.of("give %player% iron_ingot 3"));
        yaml.set(p + "tieredRewards.3.broadcast", List.of("%player% found 3!"));
        yaml.set(p + "tieredRewards.3.slotsRequired", 2);
        yaml.set(p + "tieredRewards.3.randomizeCommands", true);
        yaml.set(p + "tieredRewards.7.messages", List.of("7 found!"));
        yaml.set(p + "tieredRewards.7.commands", List.of("give %player% gold_ingot 7"));
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        List<TieredReward> rewards = hunt.getConfig().getTieredRewards();
        assertThat(rewards).hasSize(2);

        TieredReward r3 = rewards.stream().filter(r -> r.level() == 3).findFirst().orElse(null);
        assertThat(r3).isNotNull();
        assertThat(r3.messages()).containsExactly("3 found!");
        assertThat(r3.commands()).containsExactly("give %player% iron_ingot 3");
        assertThat(r3.broadcastMessages()).containsExactly("%player% found 3!");
        assertThat(r3.slotsRequired()).isEqualTo(2);
        assertThat(r3.isRandom()).isTrue();

        TieredReward r7 = rewards.stream().filter(r -> r.level() == 7).findFirst().orElse(null);
        assertThat(r7).isNotNull();
        assertThat(r7.messages()).containsExactly("7 found!");
        assertThat(r7.commands()).containsExactly("give %player% gold_ingot 7");
        assertThat(r7.slotsRequired()).isEqualTo(-1); // default
        assertThat(r7.isRandom()).isFalse(); // default
    }

    @Test
    void loadHuntConfig_noConfigSection_fallsBackToConfigService() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/noconfig.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "noconfig");
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        HuntConfig hc = hunt.getConfig();
        // Should fall back to ConfigService defaults since no config. section exists
        assertThat(hc.isHeadClickTitleEnabled()).isFalse();
        assertThat(hc.isSpinEnabled()).isTrue();
        assertThat(hc.getHintDistance()).isEqualTo(16);
        assertThat(hc.getParticlesFoundAmount()).isEqualTo(3);
    }

    // --- generateDefaultFromConfig with non-empty values ---

    @Test
    void generateDefaultFromConfig_populatesFieldsFromConfigService() throws IOException {
        // Delete existing default.yml, set up rich ConfigService stubs, then regenerate
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");
        assertThat(defaultFile.delete()).isTrue();

        lenient().when(configService.headClickMessages()).thenReturn(List.of("Click message!"));
        lenient().when(configService.headClickTitleEnabled()).thenReturn(true);
        lenient().when(configService.headClickTitleFirstLine()).thenReturn("&eTitle");
        lenient().when(configService.headClickTitleSubTitle()).thenReturn("&7Sub");
        lenient().when(configService.headClickTitleFadeIn()).thenReturn(5);
        lenient().when(configService.headClickTitleStay()).thenReturn(40);
        lenient().when(configService.headClickTitleFadeOut()).thenReturn(10);
        lenient().when(configService.headClickNotOwnSound()).thenReturn("PLING");
        lenient().when(configService.headClickAlreadyOwnSound()).thenReturn("ANVIL");
        lenient().when(configService.fireworkEnabled()).thenReturn(true);
        lenient().when(configService.headClickCommands()).thenReturn(List.of("cmd1", "cmd2"));
        lenient().when(configService.headClickEjectEnabled()).thenReturn(true);
        lenient().when(configService.headClickEjectPower()).thenReturn(3.0);
        lenient().when(configService.hologramsFoundEnabled()).thenReturn(false);
        lenient().when(configService.hologramsNotFoundEnabled()).thenReturn(true);
        lenient().when(configService.hologramsFoundLines()).thenReturn(new ArrayList<>(List.of("Found line")));
        lenient().when(configService.hologramsNotFoundLines()).thenReturn(new ArrayList<>(List.of("NotFound line")));
        lenient().when(configService.hintDistanceBlocks()).thenReturn(24);
        lenient().when(configService.hintFrequency()).thenReturn(8);
        lenient().when(configService.spinEnabled()).thenReturn(false);
        lenient().when(configService.spinSpeed()).thenReturn(2);
        lenient().when(configService.spinLinked()).thenReturn(false);
        lenient().when(configService.particlesFoundEnabled()).thenReturn(true);
        lenient().when(configService.particlesFoundType()).thenReturn("FLAME");
        lenient().when(configService.particlesFoundAmount()).thenReturn(10);
        lenient().when(configService.particlesNotFoundEnabled()).thenReturn(true);
        lenient().when(configService.particlesNotFoundType()).thenReturn("HEART");
        lenient().when(configService.particlesNotFoundAmount()).thenReturn(7);
        lenient().when(configService.tieredRewards()).thenReturn(List.of(
                new TieredReward(1, List.of("Tier msg"), List.of("tier cmd"), List.of("tier bcast"), 5, true)
        ));

        huntConfigService.generateDefaultFromConfig();

        assertThat(defaultFile).exists();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(defaultFile);

        String p = "config.";
        assertThat(yaml.getStringList(p + "headClick.messages")).containsExactly("Click message!");
        assertThat(yaml.getBoolean(p + "headClick.title.enabled")).isTrue();
        assertThat(yaml.getString(p + "headClick.title.firstLine")).isEqualTo("&eTitle");
        assertThat(yaml.getString(p + "headClick.title.subTitle")).isEqualTo("&7Sub");
        assertThat(yaml.getInt(p + "headClick.title.fadeIn")).isEqualTo(5);
        assertThat(yaml.getInt(p + "headClick.title.stay")).isEqualTo(40);
        assertThat(yaml.getInt(p + "headClick.title.fadeOut")).isEqualTo(10);
        assertThat(yaml.getString(p + "headClick.sound.found")).isEqualTo("PLING");
        assertThat(yaml.getString(p + "headClick.sound.alreadyOwn")).isEqualTo("ANVIL");
        assertThat(yaml.getBoolean(p + "headClick.firework.enabled")).isTrue();
        assertThat(yaml.getStringList(p + "headClick.commands")).containsExactly("cmd1", "cmd2");
        assertThat(yaml.getBoolean(p + "headClick.eject.enabled")).isTrue();
        assertThat(yaml.getDouble(p + "headClick.eject.power")).isEqualTo(3.0);
        assertThat(yaml.getBoolean(p + "holograms.found.enabled")).isFalse();
        assertThat(yaml.getBoolean(p + "holograms.notFound.enabled")).isTrue();
        assertThat(yaml.getStringList(p + "holograms.found.lines")).containsExactly("Found line");
        assertThat(yaml.getStringList(p + "holograms.notFound.lines")).containsExactly("NotFound line");
        assertThat(yaml.getInt(p + "hints.distance")).isEqualTo(24);
        assertThat(yaml.getInt(p + "hints.frequency")).isEqualTo(8);
        assertThat(yaml.getBoolean(p + "spin.enabled")).isFalse();
        assertThat(yaml.getInt(p + "spin.speed")).isEqualTo(2);
        assertThat(yaml.getBoolean(p + "spin.linked")).isFalse();
        assertThat(yaml.getBoolean(p + "particles.found.enabled")).isTrue();
        assertThat(yaml.getString(p + "particles.found.type")).isEqualTo("FLAME");
        assertThat(yaml.getInt(p + "particles.found.amount")).isEqualTo(10);
        assertThat(yaml.getBoolean(p + "particles.notFound.enabled")).isTrue();
        assertThat(yaml.getString(p + "particles.notFound.type")).isEqualTo("HEART");
        assertThat(yaml.getInt(p + "particles.notFound.amount")).isEqualTo(7);

        assertThat(yaml.getStringList(p + "tieredRewards.1.messages")).containsExactly("Tier msg");
        assertThat(yaml.getStringList(p + "tieredRewards.1.commands")).containsExactly("tier cmd");
        assertThat(yaml.getStringList(p + "tieredRewards.1.broadcast")).containsExactly("tier bcast");
        assertThat(yaml.getInt(p + "tieredRewards.1.slotsRequired")).isEqualTo(5);
        assertThat(yaml.getBoolean(p + "tieredRewards.1.randomizeCommands")).isTrue();
    }

    @Test
    void generateDefaultFromConfig_skipsExistingDefaultFile() throws IOException {
        // default.yml already exists from constructor
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(defaultFile);
        existing.set("displayName", "Untouched");
        existing.save(defaultFile);

        huntConfigService.generateDefaultFromConfig();

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(defaultFile);
        assertThat(reloaded.getString("displayName")).isEqualTo("Untouched");
    }

    // --- Multiple hunts interaction ---

    @Test
    void saveMultipleHunts_loadHunts_returnsAll() {
        // Delete default so we control everything
        huntConfigService.deleteHuntFile("default");

        Hunt hunt1 = new Hunt(configService, "alpha", "Alpha Hunt", HuntState.ACTIVE, 1, "CHEST");
        Hunt hunt2 = new Hunt(configService, "beta", "Beta Hunt", HuntState.INACTIVE, 2, "DIAMOND");
        Hunt hunt3 = new Hunt(configService, "gamma", "Gamma Hunt", HuntState.ARCHIVED, 3, "GOLD_BLOCK");

        huntConfigService.saveHunt(hunt1);
        huntConfigService.saveHunt(hunt2);
        huntConfigService.saveHunt(hunt3);

        List<Hunt> loaded = huntConfigService.loadHunts();

        assertThat(loaded).hasSize(3);
        assertThat(loaded).extracting(Hunt::getId).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void saveHunt_overwritesExistingFile() {
        Hunt original = new Hunt(configService, "overwrite", "Original", HuntState.ACTIVE, 1, "CHEST");
        huntConfigService.saveHunt(original);

        Hunt updated = new Hunt(configService, "overwrite", "Updated", HuntState.INACTIVE, 99, "EMERALD");
        huntConfigService.saveHunt(updated);

        File file = new File(tempDir.toFile(), "hunts/overwrite.yml");
        Hunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getDisplayName()).isEqualTo("Updated");
        assertThat(loaded.getState()).isEqualTo(HuntState.INACTIVE);
        assertThat(loaded.getPriority()).isEqualTo(99);
        assertThat(loaded.getIcon()).isEqualTo("EMERALD");
    }

    // --- Edge cases ---

    @Test
    void loadHunt_unknownState_defaultsToActive() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/badstate.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "badstate");
        yaml.set("state", "NONEXISTENT_STATE");
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        assertThat(hunt.getState()).isEqualTo(HuntState.ACTIVE);
    }

    @Test
    void loadHunt_tieredRewardsWithInvalidLevel_skipsInvalid() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/badtier.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "badtier");

        String p = "config.";
        // Valid tier
        yaml.set(p + "tieredRewards.5.messages", List.of("Level 5"));
        // Invalid tier key (not a number) - should be skipped gracefully
        yaml.set(p + "tieredRewards.notanumber.messages", List.of("Bad level"));
        yaml.save(file);

        Hunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        List<TieredReward> rewards = hunt.getConfig().getTieredRewards();
        // Only the valid tier should be loaded
        assertThat(rewards).hasSize(1);
        assertThat(rewards.get(0).level()).isEqualTo(5);
    }
}
