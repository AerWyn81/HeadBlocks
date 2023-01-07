package fr.aerwyn81.headblocks.utils.internal;

import java.util.Collection;
import java.util.UUID;

public class InternalUtils {

    public static UUID generateNewUUID(Collection<UUID> excluded) {
        var newUUID = UUID.randomUUID();
        while (excluded.contains(newUUID)) {
            newUUID = UUID.randomUUID();
        }

        return newUUID;
    }
}
