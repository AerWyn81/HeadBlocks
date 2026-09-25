package fr.aerwyn81.headblocks.data.head.visual;

public enum VisualForm {
    HEAD_BLOCK(true),
    BLOCK(true),
    ITEM_DISPLAY(false),
    BLOCK_DISPLAY(false),
    MOB(false),
    EXTERNAL(false);

    private final boolean blockBased;

    VisualForm(boolean blockBased) {
        this.blockBased = blockBased;
    }

    public boolean isBlockBased() {
        return blockBased;
    }

    public boolean isEntityBased() {
        return !blockBased;
    }

    public static VisualForm resolve(HeadContent content, RenderMode mode) {
        var kind = content == null ? ContentKind.HEAD : content.kind();
        var display = mode == RenderMode.DISPLAY;

        return switch (kind) {
            case HEAD -> display ? ITEM_DISPLAY : HEAD_BLOCK;
            case BLOCK -> display ? BLOCK_DISPLAY : BLOCK;
            case ITEM -> ITEM_DISPLAY;
            case MOB -> MOB;
            case EXTERNAL -> EXTERNAL;
        };
    }
}
