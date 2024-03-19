package net.flectone.chat.module.commands;

import net.flectone.chat.module.FCommand;
import net.flectone.chat.module.FModule;
import net.flectone.chat.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandReply extends FCommand {
    public CommandReply(FModule module, String name) {
        super(module, name);
        init();
    }

    @Override
    public void init() {
        if (!isEnabled()) return;
        register();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias,
                             @NotNull String[] args) {

        CmdSettings cmdSettings = new CmdSettings(commandSender, command);

        if (cmdSettings.isConsole()) {
            sendErrorMessage(commandSender, getModule() + ".console");
            return true;
        }

        Player lastWriter = cmdSettings.getFPlayer().getLastWriter();
        if (lastWriter == null) {
            sendErrorMessage(commandSender, this + ".no-receiver");
            return true;
        }

        String message = MessageUtil.joinArray(args, 0, " ");

        dispatchCommand(commandSender, "tell " + lastWriter.getName() + " " + message);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> ret = new ArrayList<>();
        if (args.length == 1) {
            isTabCompleteMessage(commandSender, args[0], "message", ret);
        }

        return getSortedTabComplete(ret);
    }
}
