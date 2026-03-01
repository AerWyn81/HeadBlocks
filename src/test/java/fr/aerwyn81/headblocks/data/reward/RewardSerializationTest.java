package fr.aerwyn81.headblocks.data.reward;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class RewardSerializationTest {

    @Test
    void serialize_producesCorrectMap() {
        Reward reward = new Reward(RewardType.COMMAND, "give %player% diamond");

        HashMap<String, String> map = reward.serialize();

        assertThat(map).containsEntry("type", "COMMAND");
        assertThat(map).containsEntry("value", "give %player% diamond");
    }

    @Test
    void deserialize_validLinkedHashMap_producesCorrectReward() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("type", "MESSAGE");
        map.put("value", "Hello world");

        Reward reward = Reward.deserialize(map);

        assertThat(reward.type()).isEqualTo(RewardType.MESSAGE);
        assertThat(reward.value()).isEqualTo("Hello world");
    }

    @Test
    void deserialize_invalidType_returnsUnknownReward() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("type", "INVALID_TYPE");
        map.put("value", "some value");

        Reward reward = Reward.deserialize(map);

        assertThat(reward.type()).isEqualTo(RewardType.UNKNOWN);
        assertThat(reward.value()).isEmpty();
    }

    @Test
    void deserialize_nonMapObject_returnsUnknownReward() {
        Reward reward = Reward.deserialize("not a map");

        assertThat(reward.type()).isEqualTo(RewardType.UNKNOWN);
        assertThat(reward.value()).isEmpty();
    }

    @Test
    void deserialize_emptyMap_returnsUnknownReward() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        Reward reward = Reward.deserialize(map);

        assertThat(reward.type()).isEqualTo(RewardType.UNKNOWN);
        assertThat(reward.value()).isEmpty();
    }

    @Test
    void deserialize_missingTypeKey_returnsUnknownWithValue() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("value", "give diamond");

        Reward reward = Reward.deserialize(map);

        // type stays UNKNOWN (default), value is parsed
        assertThat(reward.type()).isEqualTo(RewardType.UNKNOWN);
        assertThat(reward.value()).isEqualTo("give diamond");
    }

    @Test
    void roundTrip_deserializeOfSerialize_returnsEquivalentReward() {
        Reward original = new Reward(RewardType.BROADCAST, "Server message");

        // serialize returns HashMap; deserialize expects LinkedHashMap
        LinkedHashMap<String, String> serialized = new LinkedHashMap<>(original.serialize());
        Reward deserialized = Reward.deserialize(serialized);

        assertThat(deserialized).isEqualTo(original);
    }
}
