package fr.aerwyn81.headblocks.utils.internal;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class InternalUtilsTest {

    @Test
    void generateNewUUID_emptyCollection_returnsValidUUID() {
        UUID result = InternalUtils.generateNewUUID(Collections.emptyList());

        assertThat(result).isNotNull();
    }

    @Test
    void generateNewUUID_withExclusions_returnsUUIDNotInCollection() {
        Set<UUID> excluded = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            excluded.add(UUID.randomUUID());
        }

        UUID result = InternalUtils.generateNewUUID(excluded);

        assertThat(excluded).doesNotContain(result);
    }

    @Test
    void getKeyByValue_existingValue_returnsKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);

        assertThat(InternalUtils.getKeyByValue(map, 2)).isEqualTo("B");
    }

    @Test
    void getKeyByValue_nonExistingValue_returnsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);

        assertThat(InternalUtils.<String, Integer>getKeyByValue(map, 999)).isNull();
    }

    @Test
    void getKeyByValue_emptyMap_returnsNull() {
        Map<String, Integer> map = new HashMap<>();

        assertThat(InternalUtils.<String, Integer>getKeyByValue(map, 42)).isNull();
    }

    @Test
    void getKeyByValue_nullValueInMap_matchesNullLookup() {
        Map<String, Integer> map = new HashMap<>();
        map.put("X", null);

        assertThat(InternalUtils.getKeyByValue(map, null)).isEqualTo("X");
    }
}
