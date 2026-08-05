package fr.aerwyn81.headblocks.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class Platforms {

    private Platforms() {
    }

    public static Platform load() {
        return load(Platform.class.getClassLoader());
    }

    static Platform load(ClassLoader classLoader) {
        List<Platform> found = new ArrayList<>();
        for (Platform platform : ServiceLoader.load(Platform.class, classLoader)) {
            found.add(platform);
        }

        if (found.size() != 1) {
            throw new IllegalStateException("Expected exactly one Platform provider, found "
                    + found.stream().map(Platform::name).toList()
                    + ". This jar was not built by the paperJar or spigotJar task.");
        }

        return found.get(0);
    }
}
