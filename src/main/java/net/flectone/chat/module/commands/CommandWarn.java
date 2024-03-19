package net.flectone.chat.module.commands;

import net.flectone.chat.model.player.FPlayer;
import net.flectone.chat.module.FCommand;
import net.flectone.chat.module.FModule;
import net.flectone.chat.module.integrations.IntegrationsModule;
import net.flectone.chat.util.MessageUtil;
import net.flectone.chat.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandWarn extends FCommand {

    public CommandWarn(FModule module, String name) {
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

        database.execute(() -> asyncOnCommand(commandSender, command, alias, args));

        return true;
    }

    public void asyncOnCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias,
                               @NotNull String[] args) {

        if (args.length < 2) {
            sendUsageMessage(commandSender, alias);
            return;
        }

        String stringTime = args[1];

        if (isTimeString(stringTime) || !StringUtils.isNumeric(stringTime.substring(0, stringTime.length() - 1))) {
            sendUsageMessage(commandSender, alias);
            return;
        }

        String warnedPlayerName = args[0];
        FPlayer warnedFPlayer = playerManager.getOffline(warnedPlayerName);

        if (warnedFPlayer == null) {
            sendErrorMessage(commandSender, getModule() + ".null-player");
            return;
        }

        if (IntegrationsModule.hasPermission(warnedFPlayer.getOfflinePlayer(), getPermission() + ".bypass")) {
            sendErrorMessage(commandSender, getModule() + ".player-bypass");
            return;
        }

        int time = stringToTime(stringTime);

        if (time < -1) {
            sendErrorMessage(commandSender, getModule() + ".long-number");
            return;
        }

        CmdSettings cmdSettings = new CmdSettings(commandSender, command);

        if (cmdSettings.isHaveCooldown()) {
            cmdSettings.getFPlayer().sendCDMessage(alias, command.getName());
            return;
        }

        database.getWarns(warnedFPlayer);

        String reason = args.length > 2
                ? MessageUtil.joinArray(args, 2, " ")
                : locale.getVaultString(commandSender, this + ".default-reason");

        String serverMessage = locale.getVaultString(commandSender, this + ".server-message")
                .replace("<player>", warnedFPlayer.getMinecraftName())
                .replace("<time>", TimeUtil.convertTime(cmdSettings.getSender(), time))
                .replace("<count>", String.valueOf(warnedFPlayer.getCountWarns() + 1))
                .replace("<reason>", reason)
                .replace("<moderator>", commandSender.getName());

        for (Player pl : Bukkit.getOnlinePlayers()){
            if (pl.hasPermission("flectonechat.see.punishments"))
                pl.sendMessage(serverMessage);
        }

        String moderator = cmdSettings.getSender() != null
                ? cmdSettings.getSender().getUniqueId().toString()
                : null;

        warnedFPlayer.warn(reason, time, moderator);

        IntegrationsModule.sendDiscordWarn(warnedFPlayer.getOfflinePlayer(),
                warnedFPlayer.getWarnList().get(warnedFPlayer.getWarnList().size() - 1),
                warnedFPlayer.getCountWarns() + 1);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> ret = tabCompleteClear();
        switch (args.length) {
            case 1 -> isConfigModePlayer(args[0], ret);
            case 2 -> isFormatString(args[1], ret);
            case 3 -> isTabCompleteMessage(commandSender, args[2], "reason", ret);
        }

        return getSortedTabComplete(ret);
    }
}
