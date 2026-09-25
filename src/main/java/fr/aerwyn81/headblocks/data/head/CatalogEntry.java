package fr.aerwyn81.headblocks.data.head;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CatalogEntry(String type, String value, Map<String, Object> options, String raw) {

    public CatalogEntry {
        options = options == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    public static CatalogEntry parse(Object raw) {
        if (!(raw instanceof String text)) {
            return null;
        }

        var separator = text.indexOf(':');
        if (separator <= 0) {
            return null;
        }

        var type = text.substring(0, separator).trim().toLowerCase();
        var value = text.substring(separator + 1).trim();

        var open = value.lastIndexOf('[');
        if (open >= 0 && value.endsWith("]")) {
            var options = parseOptions(value.substring(open + 1, value.length() - 1));
            if (options != null) {
                return new CatalogEntry(type, value.substring(0, open).trim(), options, text);
            }
        }

        return new CatalogEntry(type, value, null, text);
    }

    private static Map<String, Object> parseOptions(String raw) {
        Map<String, Object> options = new LinkedHashMap<>();
        for (var pair : raw.split(",")) {
            var equals = pair.indexOf('=');
            if (equals <= 0) {
                return null;
            }
            options.put(pair.substring(0, equals).trim().toLowerCase(), pair.substring(equals + 1).trim());
        }
        return options;
    }
}
