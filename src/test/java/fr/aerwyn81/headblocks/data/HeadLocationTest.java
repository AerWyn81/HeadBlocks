package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadLocationTest {

    @Mock
    World world;

    // --- Full constructor ---

    @Test
    void fullConstructor_allFieldsSet() {
        UUID uuid = UUID.randomUUID();
        var rewards = new ArrayList<Reward>();
        rewards.add(new Reward(RewardType.COMMAND, "give %player% diamond"));

        HeadLocation hl = new HeadLocation("MyHead", uuid, "world", 10.5, 64.0, 20.5, 3, true, false, rewards);

        assertThat(hl.getName()).isEqualTo("MyHead");
        assertThat(hl.getUuid()).isEqualTo(uuid);
        assertThat(hl.getConfigWorldName()).isEqualTo("world");
        assertThat(hl.getX()).isEqualTo(10.5);
        assertThat(hl.getY()).isEqualTo(64.0);
        assertThat(hl.getZ()).isEqualTo(20.5);
        assertThat(hl.getOrderIndex()).isEqualTo(3);
        assertThat(hl.isHintSoundEnabled()).isTrue();
        assertThat(hl.isHintActionBarEnabled()).isFalse();
        assertThat(hl.getRewards()).hasSize(1);
        assertThat(hl.isCharged()).isFalse();
        assertThat(hl.getLocation()).isNull();
    }

    @Test
    void locationConstructor_setsLocationAndCharged() {
        UUID uuid = UUID.randomUUID();
        Location loc = new Location(world, 10.5, 64.0, 20.5);
        when(world.getName()).thenReturn("world");

        HeadLocation hl = new HeadLocation("Head", uuid, loc);

        assertThat(hl.getLocation()).isSameAs(loc);
        assertThat(hl.isCharged()).isTrue();
        assertThat(hl.getOrderIndex()).isEqualTo(-1);
        assertThat(hl.getRewards()).isEmpty();
    }

    // --- Name resolution ---

    @Test
    void getNameOrUnnamed_emptyName_returnsFallbackLabel() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        assertThat(hl.getNameOrUnnamed("Sans nom")).isEqualTo("Sans nom");
    }

    @Test
    void getNameOrUnnamed_hasName_colorizesName() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("&aGreen", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize("&aGreen")).thenReturn("§aGreen");

            assertThat(hl.getNameOrUnnamed("Unnamed")).isEqualTo("§aGreen");
        }
    }

    @Test
    void getRawNameOrUuid_emptyName_returnsUuidString() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        assertThat(hl.getRawNameOrUuid()).isEqualTo(uuid.toString());
    }

    @Test
    void getRawNameOrUuid_hasName_uncolorizesName() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("§aGreen", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.unColorize("§aGreen")).thenReturn("Green");

            assertThat(hl.getRawNameOrUuid()).isEqualTo("Green");
        }
    }

    @Test
    void getNameOrUuid_emptyName_returnsUuidString() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        assertThat(hl.getNameOrUuid()).isEqualTo(uuid.toString());
    }

    @Test
    void getNameOrUuid_hasName_colorizesName() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("&bBlue", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize("&bBlue")).thenReturn("§bBlue");

            assertThat(hl.getNameOrUuid()).isEqualTo("§bBlue");
        }
    }

    // --- Displayed order index ---

    @Test
    void getDisplayedOrderIndex_minusOne_returnsFallbackLabel() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        assertThat(hl.getDisplayedOrderIndex("Aucun")).isEqualTo("Aucun");
    }

    @Test
    void getDisplayedOrderIndex_positiveValue_returnsStringValue() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, 5, false, false, new ArrayList<>());

        assertThat(hl.getDisplayedOrderIndex("None")).isEqualTo("5");
    }

    // --- Setters ---

    @Test
    void setLocation_preservesCoordinatesAndWorldName() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "world", 10.5, 64.0, 20.5, -1, false, false, new ArrayList<>());
        when(world.getName()).thenReturn("newWorld");
        Location newLoc = new Location(world, 99, 99, 99);

        hl.setLocation(newLoc);

        assertThat(hl.getLocation()).isSameAs(newLoc);
        assertThat(hl.getX()).isEqualTo(99);
        assertThat(hl.getY()).isEqualTo(99);
        assertThat(hl.getZ()).isEqualTo(99);
        assertThat(hl.getConfigWorldName()).isEqualTo("newWorld");
    }

    @Test
    void addReward_appendsToList() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        hl.addReward(new Reward(RewardType.MESSAGE, "Hi"));
        hl.addReward(new Reward(RewardType.COMMAND, "cmd"));

        assertThat(hl.getRewards()).hasSize(2);
    }

    // --- fromConfig ---

    @Test
    void fromConfig_integerCoordinates_centeredByHalf() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "Test");
        config.set(key + ".location.x", 10.0);
        config.set(key + ".location.y", 64.0);
        config.set(key + ".location.z", 20.0);
        config.set(key + ".location.world", "world");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("world")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            // x and z were integers → +0.5, y is not centered
            assertThat(hl.getX()).isEqualTo(10.5);
            assertThat(hl.getZ()).isEqualTo(20.5);
            assertThat(hl.getY()).isEqualTo(64.0);
        }
    }

    @Test
    void fromConfig_nonIntegerCoordinates_notCentered() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "Test");
        config.set(key + ".location.x", 10.3);
        config.set(key + ".location.y", 64.0);
        config.set(key + ".location.z", 20.7);
        config.set(key + ".location.world", "world");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("world")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.getX()).isEqualTo(10.3);
            assertThat(hl.getZ()).isEqualTo(20.7);
        }
    }

    @Test
    void fromConfig_legacyFormat_readsWithoutLocationSubpath() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        // Legacy format: no .name key, coords at .x/.y/.z directly
        config.set(key + ".x", 5.5);
        config.set(key + ".y", 70.0);
        config.set(key + ".z", 15.5);
        config.set(key + ".world", "nether");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("nether")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.getX()).isEqualTo(5.5);
            assertThat(hl.getY()).isEqualTo(70.0);
            assertThat(hl.getZ()).isEqualTo(15.5);
            assertThat(hl.getName()).isNull(); // name key was not set
        }
    }

    @Test
    void fromConfig_withOrderIndex() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "Ordered");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "");
        config.set(key + ".orderIndex", 7);

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.getOrderIndex()).isEqualTo(7);
        }
    }

    @Test
    void fromConfig_withoutOrderIndex_defaultsToMinusOne() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "NoOrder");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.getOrderIndex()).isEqualTo(-1);
        }
    }

    @Test
    void fromConfig_hintFlags() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "Hints");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "");
        config.set(key + ".hintSound", true);
        config.set(key + ".hintActionBar", true);

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.isHintSoundEnabled()).isTrue();
            assertThat(hl.isHintActionBarEnabled()).isTrue();
        }
    }

    @Test
    void fromConfig_worldFound_setsLocationAndCharged() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "Charged");
        config.set(key + ".location.x", 10.5);
        config.set(key + ".location.y", 64.0);
        config.set(key + ".location.z", 20.5);
        config.set(key + ".location.world", "overworld");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("overworld")).thenReturn(world);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.isCharged()).isTrue();
            assertThat(hl.getLocation()).isNotNull();
            assertThat(hl.getLocation().getWorld()).isSameAs(world);
        }
    }

    @Test
    void fromConfig_worldNotFound_notCharged() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        String key = "locations." + uuid;

        config.set(key + ".name", "NoWorld");
        config.set(key + ".location.x", 0.5);
        config.set(key + ".location.y", 0);
        config.set(key + ".location.z", 0.5);
        config.set(key + ".location.world", "deleted_world");

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getWorld("deleted_world")).thenReturn(null);

            HeadLocation hl = HeadLocation.fromConfig(config, uuid);

            assertThat(hl.isCharged()).isFalse();
            assertThat(hl.getLocation()).isNull();
        }
    }

    // --- saveInConfig ---

    @Test
    void saveInConfig_writesAllFields() {
        UUID uuid = UUID.randomUUID();
        Location loc = new Location(world, 10.5, 64.0, 20.5);
        when(world.getName()).thenReturn("overworld");

        HeadLocation hl = new HeadLocation("TestHead", uuid, loc);
        hl.setOrderIndex(3);
        hl.setHintSound(true);
        hl.addReward(new Reward(RewardType.COMMAND, "give %player% diamond"));

        YamlConfiguration config = new YamlConfiguration();
        hl.saveInConfig(config);

        String key = "locations." + uuid;
        assertThat(config.getString(key + ".name")).isEqualTo("TestHead");
        assertThat(config.getDouble(key + ".location.x")).isEqualTo(10.5);
        assertThat(config.getDouble(key + ".location.y")).isEqualTo(64.0);
        assertThat(config.getDouble(key + ".location.z")).isEqualTo(20.5);
        assertThat(config.getString(key + ".location.world")).isEqualTo("overworld");
        assertThat(config.getInt(key + ".orderIndex")).isEqualTo(3);
        assertThat(config.getBoolean(key + ".hintSound")).isTrue();
        assertThat(config.get(key + ".hintActionBar")).isNull(); // false → not saved
        assertThat(config.getList(key + ".rewards")).hasSize(1);
    }

    @Test
    void saveInConfig_defaultOrderIndex_savedAsNull() {
        UUID uuid = UUID.randomUUID();
        Location loc = new Location(world, 0, 0, 0);
        lenient().when(world.getName()).thenReturn("w");

        HeadLocation hl = new HeadLocation("H", uuid, loc);
        // orderIndex is -1 by default

        YamlConfiguration config = new YamlConfiguration();
        hl.saveInConfig(config);

        assertThat(config.get("locations." + uuid + ".orderIndex")).isNull();
    }

    @Test
    void saveInConfig_noRewards_noRewardsKey() {
        UUID uuid = UUID.randomUUID();
        Location loc = new Location(world, 0, 0, 0);
        lenient().when(world.getName()).thenReturn("w");

        HeadLocation hl = new HeadLocation("H", uuid, loc);

        YamlConfiguration config = new YamlConfiguration();
        hl.saveInConfig(config);

        assertThat(config.contains("locations." + uuid + ".rewards")).isFalse();
    }

    // --- removeFromConfig ---

    @Test
    void removeFromConfig_removesSection() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        YamlConfiguration config = new YamlConfiguration();
        config.set("locations." + uuid + ".name", "Test");

        hl.removeFromConfig(config);

        assertThat(config.contains("locations." + uuid)).isFalse();
    }

    // --- Setters ---

    @Test
    void setName_updatesName() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("Old", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        hl.setName("New");

        assertThat(hl.getName()).isEqualTo("New");
    }

    @Test
    void setCharged_updatesCharged() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        assertThat(hl.isCharged()).isFalse();
        hl.setCharged(true);
        assertThat(hl.isCharged()).isTrue();
    }

    @Test
    void setHintSound_updatesFlag() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        hl.setHintSound(true);
        assertThat(hl.isHintSoundEnabled()).isTrue();
    }

    @Test
    void setHintActionBar_updatesFlag() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        hl.setHintActionBar(true);
        assertThat(hl.isHintActionBarEnabled()).isTrue();
    }

    @Test
    void setOrderIndex_updatesOrderIndex() {
        UUID uuid = UUID.randomUUID();
        HeadLocation hl = new HeadLocation("H", uuid, "w", 0, 0, 0, -1, false, false, new ArrayList<>());

        hl.setOrderIndex(10);
        assertThat(hl.getOrderIndex()).isEqualTo(10);
    }
}
