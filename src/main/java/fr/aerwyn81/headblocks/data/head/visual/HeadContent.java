package fr.aerwyn81.headblocks.data.head.visual;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record HeadContent(ContentKind kind, String value, String provider, Map<String, Object> options) {

    private static final Gson GSON = new Gson();

    public HeadContent {
        options = options == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    public static HeadContent head(String texture) {
        return new HeadContent(ContentKind.HEAD, texture == null ? "" : texture, null, null);
    }

    public static HeadContent of(ContentKind kind, String value, Map<String, Object> options) {
        return new HeadContent(kind, value, null, options);
    }

    public static HeadContent withWall(HeadContent content) {
        Map<String, Object> options = new LinkedHashMap<>(content.options());
        options.put("wall", true);
        return new HeadContent(content.kind(), content.value(), content.provider(), options);
    }

    public static HeadContent external(String provider, String value, Map<String, Object> options) {
        return new HeadContent(ContentKind.EXTERNAL, value, provider, options);
    }

    public String option(String key) {
        var raw = options.get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    public boolean optionBoolean(String key, boolean defaultValue) {
        var raw = options.get(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }

    public double optionDouble(String key, double defaultValue) {
        var raw = options.get(key);
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Integer optionInt(String key) {
        var raw = options.get(key);
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void save(ConfigurationSection parent, String key) {
        parent.set(key, null);

        var section = parent.createSection(key);
        section.set("kind", kind.name());
        section.set("value", value);
        if (provider != null) {
            section.set("provider", provider);
        }
        if (!options.isEmpty()) {
            section.createSection("options", options);
        }
    }

    public static HeadContent load(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        var kind = ContentKind.of(section.getString("kind"));
        var value = section.getString("value");
        if (kind == null || value == null) {
            return null;
        }

        Map<String, Object> options = new LinkedHashMap<>();
        var optionsSection = section.getConfigurationSection("options");
        if (optionsSection != null) {
            options.putAll(optionsSection.getValues(false));
        }

        return new HeadContent(kind, value, section.getString("provider"), options);
    }

    public String toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", kind.name());
        map.put("value", value);
        if (provider != null) {
            map.put("provider", provider);
        }
        if (!options.isEmpty()) {
            map.put("options", options);
        }
        return GSON.toJson(map);
    }

    @SuppressWarnings("unchecked")
    public static HeadContent fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());
            if (map == null) {
                return null;
            }

            var kind = ContentKind.of((String) map.get("kind"));
            var value = (String) map.get("value");
            if (kind == null || value == null) {
                return null;
            }

            var options = map.get("options") instanceof Map<?, ?> raw ? (Map<String, Object>) raw : null;
            return new HeadContent(kind, value, (String) map.get("provider"), options);
        } catch (JsonSyntaxException | ClassCastException e) {
            return null;
        }
    }

    public String describe() {
        var base = provider != null ? provider + ":" + value : kind.name().toLowerCase() + ":" + value;
        return kind == ContentKind.HEAD ? "head" : base;
    }
}
