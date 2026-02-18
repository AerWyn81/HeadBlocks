package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.internal.ExportSQLHelper;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@HBAnnotations(command = "export", permission = "headblocks.admin", args = {"database"}, alias = "e")
public class Export implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
            return true;
        }

        EnumTypeDatabase typeDatabase = EnumTypeDatabase.of(args[2]);

        if (typeDatabase == null) {
            sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cThe SQL type &e" + args[2] + " &cis not supported!"));
            return true;
        }

        String fileName = "export-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".sql";

        sender.sendMessage(MessageUtils.colorize(LanguageService.getMessage("Messages.ExportInProgress")));

        HeadBlocks.getScheduler().runAsync((task) -> {
            try {
                ExportSQLHelper.generateFile(typeDatabase, fileName);
                Thread.sleep(10000);
            } catch (Exception ex) {
                sender.sendMessage(MessageUtils.colorize(LanguageService.getMessage("Messages.ExportError") + ex.getMessage()));
            }

            sender.sendMessage(MessageUtils.colorize(LanguageService.getMessage("Messages.ExportSuccess"))
                    .replaceAll("%fileName%", fileName));
        });

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(Collections.singleton("database"));
        }

        if (args.length == 3) {
            return Stream.of(EnumTypeDatabase.values())
                    .map(Enum::name)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return new ArrayList<>();
    }
}
