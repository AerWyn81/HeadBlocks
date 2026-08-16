package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ScheduledBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.*;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PermissionRequirement;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HBHuntConfigServiceTest {
    @TempDir
    Path tempDir;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private ConfigService configService;

    @Mock
    private ServiceRegistry registry;

    @Mock
    private SchedulerAdapter scheduler;

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

        huntConfigService = new HuntConfigService(pluginProvider, configService, registry, scheduler);
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
    void loadHunts_emptyDirectory_returnsEmptyList() {
        // Delete the generated default.yml so hunts dir is empty
        File defaultFile = new File(tempDir.toFile(), "hunts/default.yml");
        assertThat(defaultFile.delete()).isTrue();

        List<HBHunt> hunts = huntConfigService.loadHunts();

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

        List<HBHunt> hunts = huntConfigService.loadHunts();

        assertThat(hunts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(hunts).extracting(HBHunt::getId).contains("default", "second");
    }

    @Test
    void loadHunts_skipsNonYmlFiles() throws IOException {
        File huntsDir = new File(tempDir.toFile(), "hunts");
        File txtFile = new File(huntsDir, "notes.txt");
        assertThat(txtFile.createNewFile()).isTrue();

        List<HBHunt> hunts = huntConfigService.loadHunts();

        // Should only contain default.yml, not notes.txt
        assertThat(hunts).isNotEmpty();
        assertThat(hunts).extracting(HBHunt::getId).doesNotContain("notes");
    }

    @Test
    void loadHunts_skipsFilesWithMissingId() throws IOException {
        File huntsDir = new File(tempDir.toFile(), "hunts");
        File badFile = new File(huntsDir, "noid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("displayName", "No ID Hunt");
        yaml.save(badFile);

        List<HBHunt> hunts = huntConfigService.loadHunts();

        // noid.yml should be skipped; default.yml should still load
        assertThat(hunts).isNotEmpty();
        assertThat(hunts).extracting(HBHunt::getId).doesNotContain((String) null);
        assertThat(hunts).extracting(HBHunt::getId).contains("default");
    }

    // --- loadHunt ---

    @Test
    void loadHunt_missingIdField_returnsNull() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/noid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("displayName", "No ID");
        yaml.save(file);

        HBHunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNull();
    }

    @Test
    void loadHunt_emptyIdField_returnsNull() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/emptyid.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "");
        yaml.set("displayName", "Empty ID");
        yaml.save(file);

        HBHunt result = huntConfigService.loadHunt(file);

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

        HBHunt result = huntConfigService.loadHunt(file);

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

        HBHunt result = huntConfigService.loadHunt(file);

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

        HBHunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        assertThat(result.getBehaviors()).isNotEmpty();
        assertThat(result.getBehaviors().getFirst().getId()).isEqualTo("free");
    }

    @Test
    void loadHunt_noBehaviorsSection_usesDefaultFreeBehavior() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/nobehavior.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "nobhunt");
        yaml.save(file);

        HBHunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        // setBehaviors with empty list should fall back to FreeBehavior
        assertThat(result.getBehaviors()).isNotEmpty();
        assertThat(result.getBehaviors().getFirst().getId()).isEqualTo("free");
    }

    // --- Requirements ---

    private ConfigurationSection writeLegacyZone(YamlConfiguration yaml) {
        ConfigurationSection zone = yaml.createSection("behaviors.zone");
        ConfigurationSection area = zone.createSection("zone");
        area.set("type", "cuboid");
        area.set("world", "world");
        area.set("min.x", 0);
        area.set("min.y", 60);
        area.set("min.z", 0);
        area.set("max.x", 10);
        area.set("max.y", 70);
        area.set("max.z", 10);
        return zone;
    }

    @Test
    void loadHunt_legacyZoneBehavior_becomesAnAreaRequirement() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/legacy.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "legacy");
        yaml.createSection("behaviors.free");
        ConfigurationSection zone = writeLegacyZone(yaml);
        zone.set("blockExit", true);
        zone.set("resetOnLeave", true);
        zone.set("messageMode", "TITLE");
        zone.set("returnPoint.world", "world");
        zone.set("returnPoint.x", 5.0);
        zone.set("returnPoint.y", 65.0);
        zone.set("returnPoint.z", 5.0);
        yaml.save(file);

        World world = mock(World.class);
        lenient().when(world.getName()).thenReturn("world");

        HBHunt result;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            result = huntConfigService.loadHunt(file);
        }

        assertThat(result).isNotNull();
        assertThat(result.getBehaviors()).extracting(Behavior::getId).containsExactly("free");

        var area = result.getRequirements().find(AreaRequirement.class);
        assertThat(area).isPresent();
        assertThat(area.get().blockExit()).isTrue();
        assertThat(area.get().resetOnLeave()).isTrue();
        assertThat(area.get().messageMode()).isEqualTo(AreaMessageMode.TITLE);
        assertThat(result.getRequirements().getMode()).isEqualTo(RequirementMode.ALL);
    }

    @Test
    void loadHunt_legacyZoneBehavior_rewritesTheFileOnce() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/legacy2.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "legacy2");
        writeLegacyZone(yaml);
        yaml.save(file);

        World world = mock(World.class);
        lenient().when(world.getName()).thenReturn("world");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            huntConfigService.loadHunt(file);
        }

        YamlConfiguration rewritten = YamlConfiguration.loadConfiguration(file);
        assertThat(rewritten.contains("behaviors.zone")).isFalse();
        assertThat(rewritten.getString("requirements.list.0.type")).isEqualTo("area");
        assertThat(rewritten.getString("requirements.list.0.area.world")).isEqualTo("world");
    }

    @Test
    void loadHunt_legacyZoneThatCannotBeRead_isCarriedOverInsteadOfDeleted() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/legacy3.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "legacy3");
        writeLegacyZone(yaml).set("blockExit", true);
        yaml.save(file);

        World world = mock(World.class);
        lenient().when(world.getName()).thenReturn("world");

        HBHunt result;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            result = huntConfigService.loadHunt(file);
        }

        assertThat(result).isNotNull();
        assertThat(result.getRequirements().isEmpty()).isTrue();

        YamlConfiguration rewritten = YamlConfiguration.loadConfiguration(file);
        assertThat(rewritten.contains("behaviors.zone")).isFalse();
        assertThat(rewritten.getString("requirements.list.0.type")).isEqualTo("area");
        assertThat(rewritten.getString("requirements.list.0.zone.world")).isEqualTo("world");
        assertThat(rewritten.getBoolean("requirements.list.0.blockExit")).isTrue();
    }

    @Test
    void loadHunt_unreadableRequirement_survivesTheNextSave() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/keep.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "keep");
        yaml.set("requirements.mode", "ALL");
        yaml.set("requirements.list.0.type", "written-by-a-newer-version");
        yaml.set("requirements.list.0.someField", 42);
        yaml.set("requirements.list.1.type", "permission");
        yaml.set("requirements.list.1.permission", "hb.vip");
        yaml.save(file);

        HBHunt result = huntConfigService.loadHunt(file);
        assertThat(result).isNotNull();
        assertThat(result.getRequirements().getRequirements()).hasSize(1);

        huntConfigService.saveHunt(result);

        YamlConfiguration rewritten = YamlConfiguration.loadConfiguration(file);
        assertThat(rewritten.getString("requirements.list.0.type")).isEqualTo("permission");
        assertThat(rewritten.getString("requirements.list.1.type")).isEqualTo("written-by-a-newer-version");
        assertThat(rewritten.getInt("requirements.list.1.someField")).isEqualTo(42);
    }

    @Test
    void loadHunt_alreadyMigrated_keepsTheRequirementsSection() throws IOException {
        File file = new File(tempDir.toFile(), "hunts/migrated.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "migrated");
        yaml.set("requirements.mode", "ANY");
        yaml.set("requirements.list.0.type", "permission");
        yaml.set("requirements.list.0.permission", "hb.vip");
        writeLegacyZone(yaml);
        yaml.save(file);

        HBHunt result = huntConfigService.loadHunt(file);

        assertThat(result).isNotNull();
        assertThat(result.getRequirements().getMode()).isEqualTo(RequirementMode.ANY);
        assertThat(result.getRequirements().getRequirements()).hasSize(1);
        assertThat(result.getRequirements().find(AreaRequirement.class)).isEmpty();
    }

    @Test
    void saveHunt_writesTheRequirements() {
        HBHunt hunt = new HBHunt(configService, "reqs", "Reqs", HuntState.ACTIVE, 1, "STONE");
        hunt.setRequirements(new RequirementSet(registry, RequirementMode.ANY,
                List.of(new PermissionRequirement(registry, "hb.vip"))));

        huntConfigService.saveHunt(hunt);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(tempDir.toFile(), "hunts/reqs.yml"));
        assertThat(yaml.getString("requirements.mode")).isEqualTo("ANY");
        assertThat(yaml.getString("requirements.list.0.type")).isEqualTo("permission");
        assertThat(yaml.getString("requirements.list.0.permission")).isEqualTo("hb.vip");
    }

    @Test
    void saveHunt_removedRequirements_areErasedFromTheFile() {
        HBHunt hunt = new HBHunt(configService, "cleared", "Cleared", HuntState.ACTIVE, 1, "STONE");
        hunt.setRequirements(new RequirementSet(registry, RequirementMode.ALL,
                List.of(new PermissionRequirement(registry, "hb.vip"))));
        huntConfigService.saveHunt(hunt);

        hunt.setRequirements(RequirementSet.empty());
        huntConfigService.saveHunt(hunt);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(tempDir.toFile(), "hunts/cleared.yml"));
        assertThat(yaml.contains("requirements")).isFalse();
    }

    @Test
    void saveAndLoad_roundTripsTheRequirements() {
        HBHunt original = new HBHunt(configService, "rtreq", "RT", HuntState.ACTIVE, 1, "STONE");
        original.setRequirements(new RequirementSet(registry, RequirementMode.ANY,
                List.of(new PermissionRequirement(registry, "hb.vip"))));

        huntConfigService.saveHunt(original);
        HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/rtreq.yml"));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getRequirements().getMode()).isEqualTo(RequirementMode.ANY);
        assertThat(loaded.getRequirements().find(PermissionRequirement.class))
                .map(PermissionRequirement::node)
                .contains("hb.vip");
    }

    // --- saveHunt and round-trip ---

    @Test
    void saveHunt_createsFileOnDisk() {
        HBHunt hunt = new HBHunt(configService, "saveme", "Save Me", HuntState.ACTIVE, 3, "EMERALD");

        huntConfigService.saveHunt(hunt);

        File savedFile = new File(tempDir.toFile(), "hunts/saveme.yml");
        assertThat(savedFile).exists();
    }

    @Test
    void saveHunt_writesCorrectValues() {
        HBHunt hunt = new HBHunt(configService, "test1", "Test One", HuntState.ARCHIVED, 7, "BEACON");

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
        HBHunt original = new HBHunt(configService, "roundtrip", "Round Trip", HuntState.INACTIVE, 42, "HOPPER");

        huntConfigService.saveHunt(original);

        File file = new File(tempDir.toFile(), "hunts/roundtrip.yml");
        HBHunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo("roundtrip");
        assertThat(loaded.getDisplayName()).isEqualTo("Round Trip");
        assertThat(loaded.getState()).isEqualTo(HuntState.INACTIVE);
        assertThat(loaded.getPriority()).isEqualTo(42);
        assertThat(loaded.getIcon()).isEqualTo("HOPPER");
    }

    @Test
    void saveAndLoad_roundTrip_preservesHuntConfigMessages() {
        HBHunt hunt = new HBHunt(configService, "msgtrip", "Msg Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        hc.setHeadClickMessages(List.of("Hello!", "World!"));

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/msgtrip.yml");
        HBHunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHeadClickMessages()).containsExactly("Hello!", "World!");
    }

    @Test
    void saveAndLoad_roundTrip_preservesHologramLines() {
        HBHunt hunt = new HBHunt(configService, "holotrip", "Holo Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        hc.setHologramsFoundLines(new ArrayList<>(List.of("&aFound!")));
        hc.setHologramsNotFoundLines(new ArrayList<>(List.of("&cNot found!")));

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/holotrip.yml");
        HBHunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHologramsFoundLines()).containsExactly("&aFound!");
        assertThat(loaded.getConfig().getHologramsNotFoundLines()).containsExactly("&cNot found!");
    }

    @Test
    void saveAndLoad_roundTrip_preservesTieredRewards() {
        HBHunt hunt = new HBHunt(configService, "tiertrip", "Tier Trip", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig hc = hunt.getConfig();
        List<TieredReward> rewards = List.of(
                new TieredReward(5, List.of("Level 5!"), List.of("give %player% diamond 1"), List.of("Broadcast!"), 3, true),
                new TieredReward(10, List.of("Level 10!"), List.of("give %player% gold_ingot 5"), List.of(), -1, false)
        );
        hc.setTieredRewards(rewards);

        huntConfigService.saveHunt(hunt);

        File file = new File(tempDir.toFile(), "hunts/tiertrip.yml");
        HBHunt loaded = huntConfigService.loadHunt(file);

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
        HBHunt hunt = new HBHunt(configService, "newbie", "Newbie", HuntState.ACTIVE, 1, "CHEST");
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
        HBHunt hunt = new HBHunt(configService, "deleteme", "Delete Me", HuntState.ACTIVE, 1, "CHEST");
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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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
    void generateDefaultFromConfig_populatesFieldsFromConfigService() {
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

        HBHunt hunt1 = new HBHunt(configService, "alpha", "Alpha Hunt", HuntState.ACTIVE, 1, "CHEST");
        HBHunt hunt2 = new HBHunt(configService, "beta", "Beta Hunt", HuntState.INACTIVE, 2, "DIAMOND");
        HBHunt hunt3 = new HBHunt(configService, "gamma", "Gamma Hunt", HuntState.ARCHIVED, 3, "GOLD_BLOCK");

        huntConfigService.saveHunt(hunt1);
        huntConfigService.saveHunt(hunt2);
        huntConfigService.saveHunt(hunt3);

        List<HBHunt> loaded = huntConfigService.loadHunts();

        assertThat(loaded).hasSize(3);
        assertThat(loaded).extracting(HBHunt::getId).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void saveHunt_overwritesExistingFile() {
        HBHunt original = new HBHunt(configService, "overwrite", "Original", HuntState.ACTIVE, 1, "CHEST");
        huntConfigService.saveHunt(original);

        HBHunt updated = new HBHunt(configService, "overwrite", "Updated", HuntState.INACTIVE, 99, "EMERALD");
        huntConfigService.saveHunt(updated);

        File file = new File(tempDir.toFile(), "hunts/overwrite.yml");
        HBHunt loaded = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

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

        HBHunt hunt = huntConfigService.loadHunt(file);

        assertThat(hunt).isNotNull();
        List<TieredReward> rewards = hunt.getConfig().getTieredRewards();
        // Only the valid tier should be loaded
        assertThat(rewards).hasSize(1);
        assertThat(rewards.getFirst().level()).isEqualTo(5);
    }

    // --- Behavior serialization round-trips ---

    @Nested
    class BehaviorSerialization {
        @Test
        void saveAndLoad_scheduledBehavior_rangeMode() {
            HBHunt hunt = new HBHunt(configService, "sched-range", "Scheduled Range", HuntState.ACTIVE, 1, "CHEST");
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
            RangeScheduleMode mode = new RangeScheduleMode(
                    LocalDateTime.of(2026, 3, 1, 10, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    List.of(slot));
            hunt.setBehaviors(List.of(new ScheduledBehavior(registry, mode)));

            huntConfigService.saveHunt(hunt);
            HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/sched-range.yml"));

            assertThat(loaded).isNotNull();
            assertThat(loaded.getBehaviors()).hasSize(1);
            Behavior behavior = loaded.getBehaviors().getFirst();
            assertThat(behavior).isInstanceOf(ScheduledBehavior.class);
            ScheduledBehavior sb = (ScheduledBehavior) behavior;
            assertThat(sb.getScheduleMode()).isInstanceOf(RangeScheduleMode.class);
            RangeScheduleMode rsm = (RangeScheduleMode) sb.getScheduleMode();
            assertThat(rsm.start()).isNotNull();
            assertThat(rsm.end()).isNotNull();
            assertThat(rsm.slots()).hasSize(1);
        }

        @Test
        void saveAndLoad_scheduledBehavior_slotsMode() {
            HBHunt hunt = new HBHunt(configService, "sched-slots", "Scheduled Slots", HuntState.ACTIVE, 1, "CHEST");
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(10, 0), LocalTime.of(16, 0));
            SlotsScheduleMode mode = new SlotsScheduleMode(
                    List.of(slot),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31));
            hunt.setBehaviors(List.of(new ScheduledBehavior(registry, mode)));

            huntConfigService.saveHunt(hunt);
            HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/sched-slots.yml"));

            assertThat(loaded).isNotNull();
            ScheduledBehavior sb = (ScheduledBehavior) loaded.getBehaviors().getFirst();
            assertThat(sb.getScheduleMode()).isInstanceOf(SlotsScheduleMode.class);
            SlotsScheduleMode ssm = (SlotsScheduleMode) sb.getScheduleMode();
            assertThat(ssm.slots()).hasSize(1);
            assertThat(ssm.activeFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(ssm.activeUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        void saveAndLoad_scheduledBehavior_recurringMode() {
            HBHunt hunt = new HBHunt(configService, "sched-rec", "Recurring", HuntState.ACTIVE, 1, "CHEST");
            RecurringScheduleMode mode = new RecurringScheduleMode(
                    RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of());
            hunt.setBehaviors(List.of(new ScheduledBehavior(registry, mode)));

            huntConfigService.saveHunt(hunt);
            HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/sched-rec.yml"));

            assertThat(loaded).isNotNull();
            ScheduledBehavior sb = (ScheduledBehavior) loaded.getBehaviors().getFirst();
            assertThat(sb.getScheduleMode()).isInstanceOf(RecurringScheduleMode.class);
            RecurringScheduleMode rsm = (RecurringScheduleMode) sb.getScheduleMode();
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.WEEK);
            assertThat(rsm.startRef()).isEqualTo("MONDAY");
            assertThat(rsm.duration()).isEqualTo(Duration.ofDays(2));
        }

        @Test
        void saveAndLoad_timedBehavior_withStartPlate() {
            // Write the YAML manually to test saveBehaviors output
            HBHunt hunt = new HBHunt(configService, "timed", "Timed", HuntState.ACTIVE, 1, "CHEST");
            World world = mock(World.class);
            when(world.getName()).thenReturn("world");
            Location plateLocation = new Location(world, 100, 64, 200);
            hunt.setBehaviors(List.of(new TimedBehavior(registry, plateLocation, true)));

            huntConfigService.saveHunt(hunt);

            // Verify the YAML structure rather than round-tripping (fromConfig needs Bukkit.getWorld)
            File file = new File(tempDir.toFile(), "hunts/timed.yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            assertThat(yaml.getBoolean("behaviors.timed.repeatable")).isTrue();
            assertThat(yaml.getString("behaviors.timed.startPlate.world")).isEqualTo("world");
            assertThat(yaml.getInt("behaviors.timed.startPlate.x")).isEqualTo(100);
            assertThat(yaml.getInt("behaviors.timed.startPlate.y")).isEqualTo(64);
            assertThat(yaml.getInt("behaviors.timed.startPlate.z")).isEqualTo(200);
        }

        @Test
        void saveAndLoad_timedBehavior_nullStartPlate() {
            HBHunt hunt = new HBHunt(configService, "timed-null", "Timed Null", HuntState.ACTIVE, 1, "CHEST");
            hunt.setBehaviors(List.of(new TimedBehavior(registry, null, false)));

            huntConfigService.saveHunt(hunt);

            File file = new File(tempDir.toFile(), "hunts/timed-null.yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            assertThat(yaml.getBoolean("behaviors.timed.repeatable")).isFalse();
            assertThat(yaml.contains("behaviors.timed.startPlate")).isFalse();
        }

        @Test
        void saveAndLoad_multipleBehaviors() {
            HBHunt hunt = new HBHunt(configService, "multi-beh", "Multi", HuntState.ACTIVE, 1, "CHEST");
            hunt.setBehaviors(List.of(
                    new FreeBehavior(),
                    new ScheduledBehavior(registry, new RangeScheduleMode(null, null, List.of()))));

            huntConfigService.saveHunt(hunt);
            HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/multi-beh.yml"));

            assertThat(loaded).isNotNull();
            assertThat(loaded.getBehaviors()).hasSize(2);
        }
    }

    // --- Location management ---

    @Nested
    class LocationManagement {
        @Test
        void loadLocationsFromHunt_emptyHunt_returnsEmpty() {
            HBHunt hunt = new HBHunt(configService, "empty-loc", "Empty", HuntState.ACTIVE, 1, "CHEST");
            huntConfigService.saveHunt(hunt);

            List<HeadLocation> locations = huntConfigService.loadLocationsFromHunt("empty-loc");

            assertThat(locations).isEmpty();
        }

        @Test
        void loadLocationsFromHunt_nonExistentHunt_returnsEmpty() {
            List<HeadLocation> locations = huntConfigService.loadLocationsFromHunt("nonexistent");

            assertThat(locations).isEmpty();
        }

        @Test
        void saveLocationInHunt_writesToCachedYaml() {
            // Create a hunt file
            HBHunt hunt = new HBHunt(configService, "loc-save", "Loc Save", HuntState.ACTIVE, 1, "CHEST");
            huntConfigService.saveHunt(hunt);

            UUID headUuid = UUID.randomUUID();
            HeadLocation headLoc = new HeadLocation("myhead", headUuid, "loc-save", "world", 10.5, 64.0, 20.5, -1, false, false, new ArrayList<>());
            huntConfigService.saveLocationInHunt("loc-save", headLoc);

            // Verify the hunt yaml file on disk contains the location key
            File file = new File(tempDir.toFile(), "hunts/loc-save.yml");
            // Note: the actual save is debounced, so read back the hunt file
            // which was modified by saveHunt (the location is in the cached yaml)
            assertThat(file.exists()).isTrue();
        }

        @Test
        void saveLocationInHunt_debouncesConcurrentSavesForTheSameHunt() throws Exception {
            HBHunt hunt = new HBHunt(configService, "loc-race", "Loc Race", HuntState.ACTIVE, 1, "CHEST");
            huntConfigService.saveHunt(hunt);
            clearInvocations(scheduler);

            int threads = 8;
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> workers = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                HeadLocation headLoc = new HeadLocation("h" + i, UUID.randomUUID(), "loc-race", "world",
                        10.5, 64.0, 20.5, -1, false, false, new ArrayList<>());
                Thread worker = new Thread(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    huntConfigService.saveLocationInHunt("loc-race", headLoc);
                });
                workers.add(worker);
                worker.start();
            }

            start.countDown();
            for (Thread worker : workers) {
                worker.join(5_000);
            }

            verify(scheduler, times(1)).runTaskLater(any(Runnable.class), anyLong());
        }

        @Test
        void debouncedSave_snapshotDoesNotRaceWithConcurrentMutations() throws Exception {
            HBHunt hunt = new HBHunt(configService, "snap-race", "Snap Race", HuntState.ACTIVE, 1, "CHEST");
            huntConfigService.saveHunt(hunt);
            clearInvocations(scheduler);

            ArgumentCaptor<Runnable> debounced = ArgumentCaptor.forClass(Runnable.class);
            doReturn(null).when(scheduler).runTaskLater(debounced.capture(), anyLong());

            huntConfigService.saveLocationInHunt("snap-race",
                    new HeadLocation("seed", UUID.randomUUID(), "snap-race", "world",
                            1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));

            List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
            AtomicBoolean running = new AtomicBoolean(true);

            Thread mutator = new Thread(() -> {
                try {
                    while (running.get()) {
                        UUID headUuid = UUID.randomUUID();
                        huntConfigService.saveLocationInHunt("snap-race",
                                new HeadLocation("h", headUuid, "snap-race", "world",
                                        1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));
                        huntConfigService.removeLocationFromHunt("snap-race", headUuid);
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
            mutator.start();

            try {
                for (int i = 0; i < 300; i++) {
                    debounced.getValue().run();
                }
            } catch (Throwable t) {
                failures.add(t);
            } finally {
                running.set(false);
                mutator.join(5_000);
            }

            assertThat(mutator.isAlive()).isFalse();
            assertThat(failures).isEmpty();
        }

        @Test
        void debouncedSave_writesCompleteYamlAtomically() {
            HBHunt hunt = new HBHunt(configService, "atomic-write", "Atomic", HuntState.ACTIVE, 1, "CHEST");
            huntConfigService.saveHunt(hunt);

            doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());

            doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            UUID headUuid = UUID.randomUUID();
            huntConfigService.saveLocationInHunt("atomic-write",
                    new HeadLocation("head", headUuid, "atomic-write", "world",
                            10.5, 64.0, 20.5, -1, false, false, new ArrayList<>()));

            File file = new File(tempDir.toFile(), "hunts/atomic-write.yml");
            YamlConfiguration written = YamlConfiguration.loadConfiguration(file);

            assertThat(written.getString("id")).isEqualTo("atomic-write");
            assertThat(written.contains("locations." + headUuid)).isTrue();
            assertThat(file.getParentFile().listFiles((dir, name) -> name.endsWith(".tmp"))).isEmpty();
        }

        @Test
        void saveHunt_duringPendingLocationSave_keepsBothRenameAndLocation() {
            huntConfigService.saveHunt(new HBHunt(configService, "rename-race", "Original", HuntState.ACTIVE, 1, "CHEST"));

            ArgumentCaptor<Runnable> debounced = ArgumentCaptor.forClass(Runnable.class);
            doReturn(null).when(scheduler).runTaskLater(debounced.capture(), anyLong());
            doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            UUID headUuid = UUID.randomUUID();
            huntConfigService.saveLocationInHunt("rename-race",
                    new HeadLocation("head", headUuid, "rename-race", "world",
                            10.5, 64.0, 20.5, -1, false, false, new ArrayList<>()));

            huntConfigService.saveHunt(new HBHunt(configService, "rename-race", "Renamed", HuntState.ACTIVE, 1, "CHEST"));

            debounced.getValue().run();

            File file = new File(tempDir.toFile(), "hunts/rename-race.yml");
            YamlConfiguration written = YamlConfiguration.loadConfiguration(file);

            assertThat(written.getString("displayName")).isEqualTo("Renamed");
            assertThat(written.contains("locations." + headUuid)).isTrue();
        }

        @Test
        void deleteHuntFile_withPendingLocationSave_doesNotRecreateFile() {
            huntConfigService.saveHunt(new HBHunt(configService, "doomed", "Doomed", HuntState.ACTIVE, 1, "CHEST"));

            ArgumentCaptor<Runnable> debounced = ArgumentCaptor.forClass(Runnable.class);
            doReturn(null).when(scheduler).runTaskLater(debounced.capture(), anyLong());

            lenient().doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            huntConfigService.saveLocationInHunt("doomed",
                    new HeadLocation("head", UUID.randomUUID(), "doomed", "world",
                            1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));

            File file = new File(tempDir.toFile(), "hunts/doomed.yml");
            huntConfigService.deleteHuntFile("doomed");
            assertThat(file).doesNotExist();

            debounced.getValue().run();

            assertThat(file).doesNotExist();
        }

        @Test
        void deleteHuntFile_thenReadLocations_doesNotServeDeletedHuntFromCache() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                huntConfigService.saveHunt(new HBHunt(configService, "reused", "Reused", HuntState.ACTIVE, 1, "CHEST"));

                doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());
                doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                }).when(scheduler).runTaskAsync(any(Runnable.class));

                huntConfigService.saveLocationInHunt("reused",
                        new HeadLocation("head", UUID.randomUUID(), "reused", "world",
                                1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));
                assertThat(huntConfigService.loadLocationsFromHunt("reused")).hasSize(1);

                huntConfigService.deleteHuntFile("reused");
                huntConfigService.saveHunt(new HBHunt(configService, "reused", "Reused", HuntState.ACTIVE, 1, "CHEST"));

                assertThat(huntConfigService.loadLocationsFromHunt("reused")).isEmpty();
            }
        }

        @Test
        void initialize_flushesPendingLocationSaveBeforeDroppingTheCache() {
            huntConfigService.saveHunt(new HBHunt(configService, "flushed", "Flushed", HuntState.ACTIVE, 1, "CHEST"));

            ArgumentCaptor<Runnable> debounced = ArgumentCaptor.forClass(Runnable.class);
            doReturn(null).when(scheduler).runTaskLater(debounced.capture(), anyLong());

            UUID headUuid = UUID.randomUUID();
            huntConfigService.saveLocationInHunt("flushed",
                    new HeadLocation("head", headUuid, "flushed", "world",
                            1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));

            File file = new File(tempDir.toFile(), "hunts/flushed.yml");
            assertThat(YamlConfiguration.loadConfiguration(file).contains("locations." + headUuid)).isFalse();

            huntConfigService.initialize();

            assertThat(YamlConfiguration.loadConfiguration(file).contains("locations." + headUuid)).isTrue();

            // The debounced task still fires afterwards; it must not undo the flush.
            debounced.getValue().run();
            assertThat(YamlConfiguration.loadConfiguration(file).contains("locations." + headUuid)).isTrue();
        }

        @Test
        void initialize_reloadsLocationsEditedOnDisk() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                huntConfigService.saveHunt(new HBHunt(configService, "edited", "Edited", HuntState.ACTIVE, 1, "CHEST"));

                doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());
                doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                }).when(scheduler).runTaskAsync(any(Runnable.class));

                huntConfigService.saveLocationInHunt("edited",
                        new HeadLocation("head", UUID.randomUUID(), "edited", "world",
                                1.5, 64.0, 2.5, -1, false, false, new ArrayList<>()));
                assertThat(huntConfigService.loadLocationsFromHunt("edited")).hasSize(1);

                File file = new File(tempDir.toFile(), "hunts/edited.yml");
                YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);
                UUID addedByHand = UUID.randomUUID();
                new HeadLocation("byHand", addedByHand, "edited", "world",
                        9.5, 70.0, 9.5, -1, false, false, new ArrayList<>()).saveInConfig(onDisk);
                assertThatCode(() -> onDisk.save(file)).doesNotThrowAnyException();

                huntConfigService.initialize();

                assertThat(huntConfigService.loadLocationsFromHunt("edited"))
                        .extracting(HeadLocation::getUuid)
                        .contains(addedByHand);
            }
        }

        @Test
        void saveHunt_afterInitialize_keepsLocationsEditedOnDisk() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                huntConfigService.saveHunt(new HBHunt(configService, "kept", "Kept", HuntState.ACTIVE, 1, "CHEST"));

                File file = new File(tempDir.toFile(), "hunts/kept.yml");
                YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);
                UUID addedByHand = UUID.randomUUID();
                new HeadLocation("byHand", addedByHand, "kept", "world",
                        9.5, 70.0, 9.5, -1, false, false, new ArrayList<>()).saveInConfig(onDisk);
                assertThatCode(() -> onDisk.save(file)).doesNotThrowAnyException();

                huntConfigService.initialize();
                huntConfigService.saveHunt(new HBHunt(configService, "kept", "Renamed", HuntState.ACTIVE, 1, "CHEST"));

                YamlConfiguration written = YamlConfiguration.loadConfiguration(file);
                assertThat(written.getString("displayName")).isEqualTo("Renamed");
                assertThat(written.contains("locations." + addedByHand)).isTrue();
            }
        }

        @Test
        void saveLocationInHunt_nonExistentHunt_doesNotThrow() {
            UUID headUuid = UUID.randomUUID();
            HeadLocation headLoc = new HeadLocation("myhead", headUuid, "nope", "world", 10.5, 64.0, 20.5, -1, false, false, new ArrayList<>());

            // Should not throw even if hunt file doesn't exist
            huntConfigService.saveLocationInHunt("nope", headLoc);
        }
    }

    // --- Migration ---

    @Nested
    class Migration {
        @Test
        void migrateLocationsFromLegacy_nullFile_noOp() {
            huntConfigService.migrateLocationsFromLegacy(null);
            // Should not throw
        }

        @Test
        void migrateLocationsFromLegacy_nonExistentFile_noOp() {
            File nonexistent = new File(tempDir.toFile(), "nope.yml");
            huntConfigService.migrateLocationsFromLegacy(nonexistent);
            // Should not throw
        }

        @Test
        void migrateLocationsFromLegacy_emptyLocations_renames() throws IOException {
            File legacyFile = new File(tempDir.toFile(), "locations.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.save(legacyFile);

            huntConfigService.migrateLocationsFromLegacy(legacyFile);

            assertThat(legacyFile.exists()).isFalse();
            assertThat(new File(tempDir.toFile(), "locations.yml.migrated").exists()).isTrue();
        }

        @Test
        void migrateLocationsFromLegacy_withLocations_renames() throws IOException {
            File legacyFile = new File(tempDir.toFile(), "locations.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            UUID headUuid = UUID.randomUUID();
            String key = "locations." + headUuid;
            yaml.set(key + ".name", "testHead");
            yaml.set(key + ".location.x", 10.5);
            yaml.set(key + ".location.y", 64.0);
            yaml.set(key + ".location.z", 20.5);
            yaml.set(key + ".location.world", "world");
            yaml.save(legacyFile);

            huntConfigService.migrateLocationsFromLegacy(legacyFile);

            assertThat(legacyFile.exists()).isFalse();
            assertThat(new File(tempDir.toFile(), "locations.yml.migrated").exists()).isTrue();
        }
    }

    // --- Yaml cache ---

    @Test
    void invalidateAllYamlCaches_allowsReloadFromDisk() throws IOException {
        HBHunt hunt = new HBHunt(configService, "cache-test", "Cache", HuntState.ACTIVE, 1, "CHEST");
        huntConfigService.saveHunt(hunt);

        // Modify file directly
        File file = new File(tempDir.toFile(), "hunts/cache-test.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("displayName", "Modified");
        yaml.save(file);

        // Without invalidation, the cached yaml would be stale
        huntConfigService.invalidateAllYamlCaches();
        HBHunt loaded = huntConfigService.loadHunt(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getDisplayName()).isEqualTo("Modified");
    }

    // --- SaveHuntConfig round-trip (only fields that saveHuntConfig persists) ---

    @Test
    void saveAndLoad_huntConfig_headClickMessages() {
        HBHunt hunt = new HBHunt(configService, "msg-cfg", "Messages", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig config = hunt.getConfig();
        config.setHeadClickMessages(List.of("&aYou found a head!", "&7Keep hunting!"));

        huntConfigService.saveHunt(hunt);
        HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/msg-cfg.yml"));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHeadClickMessages()).containsExactly("&aYou found a head!", "&7Keep hunting!");
    }

    @Test
    void saveAndLoad_huntConfig_tieredRewardsWithAllFields() {
        HBHunt hunt = new HBHunt(configService, "tier-full", "Tiers", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig config = hunt.getConfig();
        List<TieredReward> rewards = List.of(
                new TieredReward(5, List.of("5 heads!"), List.of("give %player% diamond 5"), List.of("&a%player% reached level 5!"), 5, true),
                new TieredReward(10, List.of("10 heads!"), List.of("give %player% diamond 10"), List.of(), -1, false));
        config.setTieredRewards(rewards);

        huntConfigService.saveHunt(hunt);
        HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/tier-full.yml"));

        assertThat(loaded).isNotNull();
        List<TieredReward> loadedRewards = loaded.getConfig().getTieredRewards();
        assertThat(loadedRewards).hasSize(2);

        TieredReward t5 = loadedRewards.stream().filter(r -> r.level() == 5).findFirst().orElse(null);
        assertThat(t5).isNotNull();
        assertThat(t5.messages()).containsExactly("5 heads!");
        assertThat(t5.commands()).containsExactly("give %player% diamond 5");
        assertThat(t5.broadcastMessages()).containsExactly("&a%player% reached level 5!");
        assertThat(t5.slotsRequired()).isEqualTo(5);
        assertThat(t5.isRandom()).isTrue();

        TieredReward t10 = loadedRewards.stream().filter(r -> r.level() == 10).findFirst().orElse(null);
        assertThat(t10).isNotNull();
        assertThat(t10.isRandom()).isFalse();
    }

    @Test
    void saveAndLoad_huntConfig_hologramLines() {
        HBHunt hunt = new HBHunt(configService, "holo-cfg", "Holo", HuntState.ACTIVE, 1, "CHEST");
        HuntConfig config = hunt.getConfig();
        config.setHologramsFoundLines(new ArrayList<>(List.of("&aFound!")));
        config.setHologramsNotFoundLines(new ArrayList<>(List.of("&cNot found")));

        huntConfigService.saveHunt(hunt);
        HBHunt loaded = huntConfigService.loadHunt(new File(tempDir.toFile(), "hunts/holo-cfg.yml"));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getConfig().getHologramsFoundLines()).containsExactly("&aFound!");
        assertThat(loaded.getConfig().getHologramsNotFoundLines()).containsExactly("&cNot found");
    }

    // =========================================================================
    // loadLocationsFromHunt with actual location data
    // =========================================================================

    @Nested
    class LoadLocationsWithData {
        @Test
        void loadLocationsFromHunt_withValidLocations_returnsHeadLocations() throws IOException {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                UUID head1 = UUID.randomUUID();
                UUID head2 = UUID.randomUUID();

                File huntFile = new File(tempDir.toFile(), "hunts/loc-test.yml");
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("id", "loc-test");
                yaml.set("displayName", "Loc Test");
                yaml.set("state", "ACTIVE");

                yaml.set("locations." + head1 + ".name", "head_one");
                yaml.set("locations." + head1 + ".location.x", 10.5);
                yaml.set("locations." + head1 + ".location.y", 64.0);
                yaml.set("locations." + head1 + ".location.z", 20.5);
                yaml.set("locations." + head1 + ".location.world", "world");

                yaml.set("locations." + head2 + ".name", "head_two");
                yaml.set("locations." + head2 + ".location.x", -5.5);
                yaml.set("locations." + head2 + ".location.y", 100.0);
                yaml.set("locations." + head2 + ".location.z", 0.5);
                yaml.set("locations." + head2 + ".location.world", "nether");

                yaml.save(huntFile);

                huntConfigService.invalidateAllYamlCaches();
                List<HeadLocation> locations = huntConfigService.loadLocationsFromHunt("loc-test");

                assertThat(locations).hasSize(2);
                assertThat(locations).extracting(HeadLocation::getHuntId).containsOnly("loc-test");
            }
        }

        @Test
        void loadLocationsFromHunt_withInvalidUuid_skipsInvalid() throws IOException {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                UUID validHead = UUID.randomUUID();

                File huntFile = new File(tempDir.toFile(), "hunts/inv-uuid.yml");
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("id", "inv-uuid");
                yaml.set("locations." + validHead + ".name", "valid");
                yaml.set("locations." + validHead + ".location.x", 1.5);
                yaml.set("locations." + validHead + ".location.y", 2.0);
                yaml.set("locations." + validHead + ".location.z", 3.5);
                yaml.set("locations." + validHead + ".location.world", "world");
                yaml.set("locations.not-a-valid-uuid.name", "broken");
                yaml.save(huntFile);

                huntConfigService.invalidateAllYamlCaches();
                List<HeadLocation> locations = huntConfigService.loadLocationsFromHunt("inv-uuid");

                assertThat(locations).hasSize(1);
                assertThat(locations.getFirst().getUuid()).isEqualTo(validHead);
            }
        }

        @Test
        void loadLocationsFromHunt_locationWithOrderIndex_preservesOrderIndex() throws IOException {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                UUID head = UUID.randomUUID();

                File huntFile = new File(tempDir.toFile(), "hunts/order-test.yml");
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("id", "order-test");
                yaml.set("locations." + head + ".name", "ordered");
                yaml.set("locations." + head + ".location.x", 5.5);
                yaml.set("locations." + head + ".location.y", 70.0);
                yaml.set("locations." + head + ".location.z", 15.5);
                yaml.set("locations." + head + ".location.world", "world");
                yaml.set("locations." + head + ".orderIndex", 3);
                yaml.save(huntFile);

                huntConfigService.invalidateAllYamlCaches();
                List<HeadLocation> locations = huntConfigService.loadLocationsFromHunt("order-test");

                assertThat(locations).hasSize(1);
                assertThat(locations.getFirst().getOrderIndex()).isEqualTo(3);
            }
        }
    }

    // =========================================================================
    // removeLocationFromHunt
    // =========================================================================

    @Nested
    class RemoveLocation {
        @Test
        void removeLocationFromHunt_existingLocation_removesFromYaml() throws IOException {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                UUID head = UUID.randomUUID();

                File huntFile = new File(tempDir.toFile(), "hunts/rem-test.yml");
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("id", "rem-test");
                yaml.set("locations." + head + ".name", "to_remove");
                yaml.set("locations." + head + ".location.x", 1.5);
                yaml.set("locations." + head + ".location.y", 2.0);
                yaml.set("locations." + head + ".location.z", 3.5);
                yaml.set("locations." + head + ".location.world", "world");
                yaml.save(huntFile);

                huntConfigService.invalidateAllYamlCaches();

                // Verify location is loaded before removal
                List<HeadLocation> before = huntConfigService.loadLocationsFromHunt("rem-test");
                assertThat(before).hasSize(1);

                huntConfigService.removeLocationFromHunt("rem-test", head);

                // The location is removed from the cached yaml (in memory)
                // Re-loading from the same cache should yield empty
                List<HeadLocation> after = huntConfigService.loadLocationsFromHunt("rem-test");
                assertThat(after).isEmpty();
            }
        }

        @Test
        void removeLocationFromHunt_nonExistentHunt_doesNotThrow() {
            huntConfigService.removeLocationFromHunt("does-not-exist", UUID.randomUUID());
        }

        @Test
        void removeLocationFromHunt_nonExistentLocation_doesNotAffectOthers() throws IOException {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);

                UUID existing = UUID.randomUUID();
                UUID nonExistent = UUID.randomUUID();

                File huntFile = new File(tempDir.toFile(), "hunts/rem-safe.yml");
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("id", "rem-safe");
                yaml.set("locations." + existing + ".name", "keeper");
                yaml.set("locations." + existing + ".location.x", 1.5);
                yaml.set("locations." + existing + ".location.y", 2.0);
                yaml.set("locations." + existing + ".location.z", 3.5);
                yaml.set("locations." + existing + ".location.world", "world");
                yaml.save(huntFile);

                huntConfigService.invalidateAllYamlCaches();
                huntConfigService.removeLocationFromHunt("rem-safe", nonExistent);

                // Existing location should still be present (cache not invalidated, same yaml)
                List<HeadLocation> remaining = huntConfigService.loadLocationsFromHunt("rem-safe");
                assertThat(remaining).hasSize(1);
                assertThat(remaining.getFirst().getUuid()).isEqualTo(existing);
            }
        }
    }
}
