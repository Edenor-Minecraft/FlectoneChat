package net.flectone.commands;

import net.flectone.misc.commands.FCommand;
import net.flectone.misc.commands.FTabCompleter;
import net.flectone.utils.ObjectUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static net.flectone.managers.FileManager.locale;

public class CommandBall implements FTabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        FCommand fCommand = new FCommand(commandSender, command.getName(), s, strings);

        if (fCommand.isInsufficientArgs(1)
                || fCommand.isHaveCD()
                || fCommand.isMuted()) return true;

        List<String> answers = locale.getStringList("command.ball.format");

        Random random = new Random();
        int randomPer = random.nextInt(0, answers.size());

        String formatString = locale.getString("command.ball.message")
                .replace("<player>", fCommand.getSenderName())
                .replace("<answer>", answers.get(randomPer));

        fCommand.sendGlobalMessage(formatString, ObjectUtil.toString(strings));

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        wordsList.clear();

        if (strings.length == 1) {
            isStartsWith(strings[0], "(message)");
        }

        Collections.sort(wordsList);

        return wordsList;
    }

    @NotNull
    @Override
    public String getCommandName() {
        return "ball";
    }
}
