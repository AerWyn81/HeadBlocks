package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextDisplayRenderer extends DisplayRenderer {

    private static final double CHAR_WIDTH = 0.125;
    private static final double LINE_HEIGHT = 0.3;
    private static final double MIN_SIZE = 0.4;

    @Override
    protected Display spawnDisplay(Location anchor, HeadContent content, float scale) {
        var display = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, TextDisplay.class);
        display.setText(MessageUtils.colorize(String.join("\n", lines(content))));
        display.setBillboard(billboardOf(content));
        display.setShadowed(content.optionBoolean("shadow", false));

        var background = backgroundOf(content.option("background"));
        if (background != null) {
            display.setBackgroundColor(background);
        }

        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()));
        return display;
    }

    @Override
    protected double size(float scale) {
        return MIN_SIZE * scale;
    }

    @Override
    protected double width(HeadContent content, float scale) {
        var longest = Arrays.stream(lines(content)).mapToInt(line -> MessageUtils.unColorize(line).length()).max().orElse(0);
        return Math.max(MIN_SIZE, longest * CHAR_WIDTH) * scale;
    }

    @Override
    protected double height(HeadContent content, float scale) {
        return Math.max(MIN_SIZE, lines(content).length * LINE_HEIGHT) * scale;
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (entity instanceof TextDisplay display && display.getBillboard() == Display.Billboard.FIXED) {
                super.spin(List.of(display), angle, settings, periodTicks);
            }
        }
    }

    static String[] lines(HeadContent content) {
        return content.value().replace("\\n", "\n").split("\n");
    }

    static Display.Billboard billboardOf(HeadContent content) {
        var raw = content.option("billboard");
        if (raw == null) {
            return Display.Billboard.CENTER;
        }

        try {
            return Display.Billboard.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Display.Billboard.CENTER;
        }
    }

    static Color backgroundOf(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        if (raw.equalsIgnoreCase("none")) {
            return Color.fromARGB(0, 0, 0, 0);
        }

        var hex = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            var value = Long.parseLong(hex, 16);
            return hex.length() == 8 ? Color.fromARGB((int) value) : Color.fromRGB((int) value).setAlpha(255);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
