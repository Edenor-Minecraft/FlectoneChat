package net.flectone.commands;

import net.flectone.custom.FCommands;
import net.flectone.custom.FPlayer;
import net.flectone.managers.FPlayerManager;
import net.flectone.custom.FTabCompleter;
import net.flectone.utils.ObjectUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CommandReply extends FTabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        FCommands fCommand = new FCommands(commandSender, command.getName(), s, strings);

        if(fCommand.isConsoleMessage()) return true;

        FPlayer secondFPlayer = FPlayerManager.getPlayer(fCommand.getFPlayer().getLastWriter());

        if(secondFPlayer == null){
            fCommand.sendMeMessage("command.null-player");
            return true;
        }

        if(!secondFPlayer.isOnline()){
            fCommand.sendMeMessage("command.reply.no-receiver");
            return true;
        }

        if(fCommand.getFPlayer().isIgnored(secondFPlayer.getPlayer())){
            fCommand.sendMeMessage("command.you_ignore");
            return true;
        }

        if(secondFPlayer.isIgnored(fCommand.getPlayer())){
            fCommand.sendMeMessage("command.he_ignore");
            return true;
        }

        if(fCommand.isHaveCD()) return true;

        if(fCommand.isMuted()) return true;

        String message = ObjectUtil.toString(strings);
        fCommand.sendTellMessage(commandSender, secondFPlayer.getPlayer(), "send", message);
        fCommand.sendTellMessage(secondFPlayer.getPlayer(), commandSender, "get", message);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        wordsList.clear();

        if(strings.length == 1){
            isStartsWith(strings[0], "(message)");
        }

        Collections.sort(wordsList);

        return wordsList;
    }
}
