package fr.aerwyn81.headblocks.services.gui.types.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The pieces every requirement menu draws the same way: the border and the back button.
 */
final class RequirementMenus {
    private RequirementMenus() {
    }

    static HBMenu newMenu(ServiceRegistry registry, String titleKey, int rows) {
        return new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message(titleKey), false, rows);
    }

    static ItemGUI borderItem(ServiceRegistry registry) {
        return new ItemGUI(registry.getConfigService().guiBorderIcon().setName("§7").toItemStack());
    }

    static void fillBorders(ServiceRegistry registry, HBMenu menu, int rows) {
        IntStream.range(0, rows * 9).forEach(index -> menu.setItem(0, index, borderItem(registry)));
    }

    static void attachBackButton(ServiceRegistry registry, HBMenu menu, Consumer<Player> onBack) {
        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> onBack.accept((Player) event.getWhoClicked()));
            }
            return null;
        });
    }
}
