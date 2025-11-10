package fr.aerwyn81.headblocks.utils.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InternalUtils {

    public static UUID generateNewUUID(Collection<UUID> excluded) {
        var newUUID = UUID.randomUUID();
        while (excluded.contains(newUUID)) {
            newUUID = UUID.randomUUID();
        }

        return newUUID;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
