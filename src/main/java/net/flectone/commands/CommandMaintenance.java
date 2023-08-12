package net.flectone.commands;

import net.flectone.integrations.discordsrv.FDiscordSRV;
import net.flectone.misc.commands.FCommand;
import net.flectone.misc.commands.FTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.flectone.managers.FileManager.locale;
import static net.flectone.managers.FileManager.config;

public class CommandMaintenance implements FTabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        FCommand fCommand = new FCommand(commandSender, command.getName(), s, strings);

        if (fCommand.isInsufficientArgs(1)) return true;

        if (!strings[0].equalsIgnoreCase("on") && !strings[0].equalsIgnoreCase("off")) {
            fCommand.sendUsageMessage();
            return true;
        }

        boolean haveMaintenance = config.getBoolean("command.maintenance.enable");

        if (haveMaintenance && strings[0].equalsIgnoreCase("on")) {
            fCommand.sendMeMessage("command.maintenance.turned-on.already");
            return true;
        }

        if (!haveMaintenance && strings[0].equalsIgnoreCase("off")) {
            fCommand.sendMeMessage("command.maintenance.turned-off.not");
            return true;
        }

        if (fCommand.isHaveCD() || fCommand.isMuted()) return true;

        haveMaintenance = strings[0].equalsIgnoreCase("on");

        if (haveMaintenance) {
            Set<Player> playerSet = new HashSet<>(Bukkit.getOnlinePlayers());

            String maintenancePermission = config.getString("command.maintenance.permission");
            String kickedMessage = locale.getFormatString("command.maintenance.kicked-message", null);

            playerSet.stream().parallel()
                    .filter(player -> !player.isOp() && !player.hasPermission(maintenancePermission) && player.isOnline())
                    .forEach(player -> player.kickPlayer(kickedMessage));
        }

        String maintenanceMessage = "command.maintenance.turned-" + strings[0].toLowerCase() + ".message";

        FDiscordSRV.sendModerationMessage(locale.getString(maintenanceMessage));

        fCommand.sendMeMessage(maintenanceMessage);
        config.set("command.maintenance.enable", haveMaintenance);
        config.save();

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        wordsList.clear();

        if (strings.length == 1) {
            isStartsWith(strings[0], "on");
            isStartsWith(strings[0], "off");
        }

        return wordsList;
    }

    @NotNull
    @Override
    public String getCommandName() {
        return "maintenance";
    }
}
