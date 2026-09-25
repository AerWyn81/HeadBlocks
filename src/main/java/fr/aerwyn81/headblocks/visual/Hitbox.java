package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;

public record Hitbox(double width, double height) {

    public static Hitbox of(HeadContent content, RenderSettings settings, double width, double height) {
        return new Hitbox(content.optionDouble("width", width) * settings.scale(), content.optionDouble("height", height) * settings.scale());
    }
}
