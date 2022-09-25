package fr.aerwyn81.headblocks.utils.gui.pagination;

import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;

/**
 * All credits of this code go to @SamJakob (SpiGUI)
 * <a href="https://github.com/SamJakob/SpiGUI">...</a>
 */
public interface HBPaginationButtonBuilder {
    ItemGUI buildPaginationButton(HBPaginationButtonType type, HBMenu inventory);
}