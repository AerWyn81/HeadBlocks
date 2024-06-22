package fr.aerwyn81.headblocks.utils.chat;

import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ChatPageUtils  {
    private final boolean isConsoleSender;
    private final int pageHeight;

    private BaseComponent lineTitle;
    private Player player;
    private int pageNumber;
    private String command;
    private int totalPage;
    private int size;

    private final ArrayList<BaseComponent[]> components;

    public ChatPageUtils(CommandSender sender) {
        this.components = new ArrayList<>();

        if (sender instanceof Player) {
            isConsoleSender = false;
            this.player = (Player) sender;
            pageHeight = 8;
        } else {
            pageHeight = Integer.MAX_VALUE;
            isConsoleSender = true;
        }
    }

    public int getFirstPos() {
        return ((pageNumber - 1) * pageHeight);
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public int getSize() {
        return size;
    }

    public ChatPageUtils entriesCount(int size) {
        this.size = size;

        totalPage = (size / pageHeight) + (size % pageHeight == 0 ? 0 : 1);
        pageNumber = (totalPage < pageNumber) ? totalPage : Math.max(pageNumber, 1);
        return this;
    }

    public ChatPageUtils currentPage(String[] args) {
        if (args.length == 1) {
            pageNumber = 1;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                pageNumber = NumberUtils.createInteger(args[args.length - 1]);
            } catch (NumberFormatException exception) {
                pageNumber = 1;
            }
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
        } else {
            pageNumber = 1;
        }

        if (totalPage != 0) {
            pageNumber = Math.min(totalPage, pageNumber);
        }

        return this;
    }

    public void addTitleLine(BaseComponent component) {
        this.lineTitle = component;
    }

    public void addLine(BaseComponent... components) {
        this.components.add(components);
    }

    public void addPageLine(String command) {
        this.command = command;
    }

    public void build() {
        if (player != null) {
            if (lineTitle != null) {
                player.spigot().sendMessage(lineTitle);
            }

            for (BaseComponent[] bc : components) {
                player.spigot().sendMessage(bc);
            }

            if (!isConsoleSender && command != null && size > pageHeight) {
                TextComponent c1 = new TextComponent(MessageUtils.colorize(LanguageService.getMessage("Chat.PreviousPage")));
                c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks " + command + " " + (pageNumber - 1)));
                c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.PreviousPage"))));

                TextComponent c2 = new TextComponent(MessageUtils.colorize(LanguageService.getMessage("Chat.NextPage")));
                c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks " + command + " " + (pageNumber + 1)));
                c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.NextPage"))));

                player.spigot().sendMessage(c1, new TextComponent(LanguageService.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))), c2);
            }
        }
    }
}
