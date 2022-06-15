package fr.aerwyn81.headblocks.utils;

import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import net.md_5.bungee.api.chat.*;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ChatPageUtils  {
    private final LanguageHandler languageHandler;
    private final boolean isConsoleSender;
    private final int pageHeight;

    private BaseComponent lineTitle;
    private Player player;
    private int pageNumber;
    private String command;
    private int totalPage;
    private int size;

    private final ArrayList<BaseComponent[]> components;

    public ChatPageUtils(LanguageHandler languageHandler, CommandSender sender) {
        this.languageHandler = languageHandler;

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
        return this;
    }

    public ChatPageUtils currentPage(String[] args) {
        totalPage = (size / pageHeight) + (size % pageHeight == 0 ? 0 : 1);

        if (args.length == 1) {
            pageNumber = 1;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                pageNumber = NumberUtils.createInteger(args[args.length - 1]);
                pageNumber = (totalPage < pageNumber) ? totalPage : Math.max(pageNumber, 1);
            } catch (NumberFormatException exception) {
                pageNumber = 1;
            }
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
        } else {
            pageNumber = 1;
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
                TextComponent c1 = new TextComponent(MessageUtils.colorize(languageHandler.getMessage("Chat.PreviousPage")));
                c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb " + command + " " + (pageNumber - 1)));
                c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.PreviousPage")).create()));

                TextComponent c2 = new TextComponent(MessageUtils.colorize(languageHandler.getMessage("Chat.NextPage")));
                c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb " + command + " " + (pageNumber + 1)));
                c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NextPage")).create()));

                player.spigot().sendMessage(c1, new TextComponent(languageHandler.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))), c2);
            }
        }
    }
}
