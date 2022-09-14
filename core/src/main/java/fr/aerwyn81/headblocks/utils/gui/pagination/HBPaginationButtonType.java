package fr.aerwyn81.headblocks.utils.gui.pagination;

/**
 * All credits of this code go to @SamJakob (SpiGUI)
 * https://github.com/SamJakob/SpiGUI
 */
public enum HBPaginationButtonType {

    BACK_BUTTON(0),
    PREV_BUTTON(3),
    CURRENT_BUTTON(4),
    NEXT_BUTTON(5),
    CLOSE_BUTTON(8),
    UNASSIGNED(1);

    private final int slot;

    HBPaginationButtonType(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public static HBPaginationButtonType forSlot(int slot) {
        for (HBPaginationButtonType buttonType : HBPaginationButtonType.values()) {
            if (buttonType.slot == slot) return buttonType;
        }

        return HBPaginationButtonType.UNASSIGNED;
    }
}
