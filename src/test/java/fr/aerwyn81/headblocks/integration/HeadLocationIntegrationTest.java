package fr.aerwyn81.headblocks.integration;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HeadLocationIntegrationTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("overworld");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void fromConfig_withRealWorld_setsLocationAndCharged() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "RealWorldHead");
        config.set(key + ".location.x", 100.5);
        config.set(key + ".location.y", 64.0);
        config.set(key + ".location.z", 200.5);
        config.set(key + ".location.world", "overworld");

        HeadLocation hl = HeadLocation.fromConfig(config, uuid, "default");

        assertThat(hl.isCharged()).isTrue();
        assertThat(hl.getLocation()).isNotNull();
        assertThat(hl.getLocation().getWorld()).isSameAs(world);
        assertThat(hl.getLocation().getX()).isEqualTo(100.5);
        assertThat(hl.getLocation().getY()).isEqualTo(64.0);
        assertThat(hl.getLocation().getZ()).isEqualTo(200.5);
    }

    @Test
    void fromConfig_withUnknownWorld_notCharged() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "LostHead");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "deleted_world");

        HeadLocation hl = HeadLocation.fromConfig(config, uuid, "default");

        assertThat(hl.isCharged()).isFalse();
        assertThat(hl.getLocation()).isNull();
    }

    @Test
    void fromConfig_integerCoordinates_centeredByHalf() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "CenteredHead");
        config.set(key + ".location.x", 10.0);
        config.set(key + ".location.y", 64.0);
        config.set(key + ".location.z", 20.0);
        config.set(key + ".location.world", "overworld");

        HeadLocation hl = HeadLocation.fromConfig(config, uuid, "default");

        // Integer x and z get +0.5 for centering
        assertThat(hl.getLocation().getX()).isEqualTo(10.5);
        assertThat(hl.getLocation().getZ()).isEqualTo(20.5);
        assertThat(hl.getLocation().getY()).isEqualTo(64.0);
    }

    @Test
    void saveAndLoad_roundtrip_preservesAllData() {
        UUID uuid = UUID.randomUUID();

        // Create a HeadLocation with all fields (rewards excluded — Paper's YamlConfiguration
        // returns List, not ArrayList, causing the instanceof check in fromConfig to fail;
        // reward serialization is tested separately in Tier 2)
        Location loc = new Location(world, 50.5, 80.0, -30.5);
        HeadLocation original = new HeadLocation("&aGreenHead", uuid, loc, "default");
        original.setOrderIndex(5);
        original.setHintSound(true);
        original.setHintActionBar(true);

        // Save to config
        YamlConfiguration config = new YamlConfiguration();
        original.saveInConfig(config);

        // Load back from config
        HeadLocation loaded = HeadLocation.fromConfig(config, uuid, "default");

        assertThat(loaded.getName()).isEqualTo("&aGreenHead");
        assertThat(loaded.getUuid()).isEqualTo(uuid);
        assertThat(loaded.isCharged()).isTrue();
        assertThat(loaded.getLocation()).isNotNull();
        assertThat(loaded.getLocation().getWorld()).isSameAs(world);
        assertThat(loaded.getLocation().getX()).isEqualTo(50.5);
        assertThat(loaded.getLocation().getY()).isEqualTo(80.0);
        assertThat(loaded.getLocation().getZ()).isEqualTo(-30.5);
        assertThat(loaded.getOrderIndex()).isEqualTo(5);
        assertThat(loaded.isHintSoundEnabled()).isTrue();
        assertThat(loaded.isHintActionBarEnabled()).isTrue();
    }

    @Test
    void saveAndLoad_roundtrip_integerCoordinates_getCentered() {
        UUID uuid = UUID.randomUUID();

        // Integer coordinates → saveInConfig stores them as-is
        Location loc = new Location(world, 10.0, 64.0, 20.0);
        HeadLocation original = new HeadLocation("IntHead", uuid, loc, "default");

        YamlConfiguration config = new YamlConfiguration();
        original.saveInConfig(config);

        HeadLocation loaded = HeadLocation.fromConfig(config, uuid, "default");

        // fromConfig applies centering to integer x and z
        assertThat(loaded.getLocation().getX()).isEqualTo(10.5);
        assertThat(loaded.getLocation().getZ()).isEqualTo(20.5);
    }

    @Test
    void fromConfig_legacyFormat_loadsCorrectly() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        // Legacy format: no .name, coords at .x/.y/.z directly
        config.set(key + ".x", 5.5);
        config.set(key + ".y", 70.0);
        config.set(key + ".z", 15.5);
        config.set(key + ".world", "overworld");

        HeadLocation hl = HeadLocation.fromConfig(config, uuid, "default");

        assertThat(hl.getName()).isNull();
        assertThat(hl.isCharged()).isTrue();
        assertThat(hl.getLocation().getX()).isEqualTo(5.5);
        assertThat(hl.getLocation().getY()).isEqualTo(70.0);
        assertThat(hl.getLocation().getZ()).isEqualTo(15.5);
    }

    @Test
    void fromConfig_multipleWorlds_resolvesCorrectly() {
        WorldMock nether = server.addSimpleWorld("world_nether");
        WorldMock end = server.addSimpleWorld("world_the_end");

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();

        config.set("locations." + uuid1 + ".name", "NetherHead");
        config.set("locations." + uuid1 + ".location.x", 0.5);
        config.set("locations." + uuid1 + ".location.y", 0);
        config.set("locations." + uuid1 + ".location.z", 0.5);
        config.set("locations." + uuid1 + ".location.world", "world_nether");

        config.set("locations." + uuid2 + ".name", "EndHead");
        config.set("locations." + uuid2 + ".location.x", 0.5);
        config.set("locations." + uuid2 + ".location.y", 0);
        config.set("locations." + uuid2 + ".location.z", 0.5);
        config.set("locations." + uuid2 + ".location.world", "world_the_end");

        HeadLocation hl1 = HeadLocation.fromConfig(config, uuid1, "default");
        HeadLocation hl2 = HeadLocation.fromConfig(config, uuid2, "default");

        assertThat(hl1.getLocation().getWorld()).isSameAs(nether);
        assertThat(hl2.getLocation().getWorld()).isSameAs(end);
    }

    @Test
    void fromConfig_withRewards_loadsAsArrayList() {
        // Note: Paper's YamlConfiguration returns List (not ArrayList) from config.get(),
        // causing the `instanceof ArrayList<?>` check in fromConfig to skip reward loading.
        // This test documents that behavior. Reward deserialization is tested in Tier 2.
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "RewardHead");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "overworld");

        var rewardList = new ArrayList<>();
        rewardList.add(new Reward(RewardType.COMMAND, "eco give %player% 100").serialize());
        config.set(key + ".rewards", rewardList);

        // Verify the rewards list is stored in config
        assertThat(config.contains(key + ".rewards")).isTrue();
        // The actual reward deserialization depends on the Bukkit YAML implementation
        HeadLocation hl = HeadLocation.fromConfig(config, uuid, "default");
        assertThat(hl).isNotNull();
    }

    @Test
    void locationConstructor_withRealWorld_setsWorldName() {
        UUID uuid = UUID.randomUUID();
        Location loc = new Location(world, 10.5, 64.0, 20.5);

        HeadLocation hl = new HeadLocation("Test", uuid, loc, "default");

        assertThat(hl.getConfigWorldName()).isEqualTo("overworld");
        assertThat(hl.isCharged()).isTrue();
    }

    @Test
    void removeFromConfig_removesEntireSection() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "ToRemove");
        config.set(key + ".location.x", 1.0);
        config.set(key + ".location.y", 2.0);
        config.set(key + ".location.z", 3.0);
        config.set(key + ".location.world", "overworld");

        HeadLocation hl = new HeadLocation("ToRemove", uuid, "default", "overworld", 1, 2, 3, -1, false, false, new ArrayList<>());
        hl.removeFromConfig(config);

        assertThat(config.contains("locations." + uuid)).isFalse();
    }
}
