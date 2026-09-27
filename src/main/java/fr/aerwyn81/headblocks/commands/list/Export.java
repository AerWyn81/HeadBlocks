package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.utils.internal.ExportSQLHelper;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@HBAnnotations(command = "export", permission = "headblocks.admin", args = {"database"}, alias = "e")
public class Export implements Cmd {
    private final ServiceRegistry registry;

    public Export(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ErrorCommand"));
            return;
        }

        EnumTypeDatabase typeDatabase = EnumTypeDatabase.of(args[2]);

        if (typeDatabase == null) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cThe SQL type &e" + args[2] + " &cis not supported!"));
            return;
        }

        String fileName = "export-" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".sql";

        sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Messages.ExportInProgress")));

        HeadBlocks.getScheduler().runTaskAsync(() -> {
            try {
                ExportSQLHelper.generateFile(registry, typeDatabase, fileName);
            } catch (Exception ex) {
                sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Messages.ExportError") + ex.getMessage()));
                return;
            }

            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Messages.ExportSuccess"))
                    .replace("%fileName%", fileName));
        });
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
