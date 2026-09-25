package fr.aerwyn81.headblocks.data.head.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VisualFormTest {

    @ParameterizedTest
    @CsvSource({
            "HEAD, BLOCK, HEAD_BLOCK",
            "HEAD, DISPLAY, ITEM_DISPLAY",
            "BLOCK, BLOCK, BLOCK",
            "BLOCK, DISPLAY, BLOCK_DISPLAY",
            "ITEM, BLOCK, ITEM_DISPLAY",
            "ITEM, DISPLAY, ITEM_DISPLAY",
            "MOB, BLOCK, MOB",
            "MOB, DISPLAY, MOB",
            "EXTERNAL, BLOCK, EXTERNAL",
            "EXTERNAL, DISPLAY, EXTERNAL"
    })
    void resolve_followsTheContentAndTheMode(ContentKind kind, RenderMode mode, VisualForm expected) {
        var content = new HeadContent(kind, "value", kind == ContentKind.EXTERNAL ? "plugin" : null, null);

        assertThat(VisualForm.resolve(content, mode)).isEqualTo(expected);
    }

    @Test
    void resolve_legacyHeadWithoutContent_isAHead() {
        assertThat(VisualForm.resolve(null, RenderMode.BLOCK)).isEqualTo(VisualForm.HEAD_BLOCK);
        assertThat(VisualForm.resolve(null, RenderMode.DISPLAY)).isEqualTo(VisualForm.ITEM_DISPLAY);
    }

    @Test
    void onlyBlockFormsAreBlockBased() {
        assertThat(VisualForm.HEAD_BLOCK.isBlockBased()).isTrue();
        assertThat(VisualForm.BLOCK.isBlockBased()).isTrue();
        assertThat(VisualForm.ITEM_DISPLAY.isEntityBased()).isTrue();
        assertThat(VisualForm.BLOCK_DISPLAY.isEntityBased()).isTrue();
        assertThat(VisualForm.MOB.isEntityBased()).isTrue();
        assertThat(VisualForm.EXTERNAL.isEntityBased()).isTrue();
    }

    @Test
    void renderMode_of_isCaseInsensitiveAndNullSafe() {
        assertThat(RenderMode.of("display")).isEqualTo(RenderMode.DISPLAY);
        assertThat(RenderMode.of("Block")).isEqualTo(RenderMode.BLOCK);
        assertThat(RenderMode.of("floating")).isNull();
        assertThat(RenderMode.of(null)).isNull();
    }

    @Test
    void renderMode_next_toggles() {
        assertThat(RenderMode.BLOCK.next()).isEqualTo(RenderMode.DISPLAY);
        assertThat(RenderMode.DISPLAY.next()).isEqualTo(RenderMode.BLOCK);
    }

    @Test
    void contentKind_of_isCaseInsensitiveAndNullSafe() {
        assertThat(ContentKind.of("mob")).isEqualTo(ContentKind.MOB);
        assertThat(ContentKind.of("painting")).isNull();
        assertThat(ContentKind.of(null)).isNull();
    }
}
