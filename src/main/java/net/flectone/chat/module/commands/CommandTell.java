package net.flectone.chat.module.commands;

import net.flectone.chat.FlectoneChat;
import net.flectone.chat.manager.FPlayerManager;
import net.flectone.chat.model.player.FPlayer;
import net.flectone.chat.model.player.Settings;
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

import static net.flectone.chat.manager.FileManager.commands;
import static net.flectone.chat.manager.FileManager.locale;

public class CommandTell extends FCommand {
    public CommandTell(FModule module, String name) {
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

        FlectoneChat.getDatabase().execute(() ->
                asyncOnCommand(commandSender, command, alias, args));

        return true;
    }

    public void asyncOnCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias,
                               @NotNull String[] args) {

        if (args.length < 2) {
            sendUsageMessage(commandSender, alias);
            return;
        }

        String receiver = args[0];
        FPlayer fReceiver = FPlayerManager.getOffline(receiver);
        if (fReceiver == null) {
            sendMessage(commandSender, getModule() + ".null-player");
            return;
        }

        CmdSettings cmdSettings = new CmdSettings(commandSender, command);

        if (cmdSettings.isHaveCooldown()) {
            sendCDMessage(cmdSettings.getSender(), alias, cmdSettings.getCooldownTime());
            return;
        }

        if (cmdSettings.isMuted()) {
            sendMutedMessage(cmdSettings.getFPlayer());
            return;
        }

        if (cmdSettings.isDisabled()) {
            sendMessage(commandSender, getModule() + ".you-disabled");
            return;
        }

        String message = MessageUtil.joinArray(args, 1, " ");

        if (!fReceiver.getOfflinePlayer().isOnline() && commands.getBoolean("mail.enable")) {
            dispatchCommand(commandSender, "mail " + receiver + " " + message);
            return;
        }

        FlectoneChat.getDatabase().getSettings(fReceiver);

        String commandTellValue = fReceiver.getSettings().getValue(Settings.Type.COMMAND_TELL);

        if (commandTellValue != null && commandTellValue.equals("-1")) {
            sendMessage(commandSender, getModule() + ".he-disabled");
            return;
        }

        if (!cmdSettings.isConsole()) {
            Player sender = cmdSettings.getSender();
            if (sender.getName().equalsIgnoreCase(receiver)) {
                String myselfMessage = locale.getVaultString(commandSender, this + ".myself")
                        .replace("<message>", message);

                commandSender.sendMessage(MessageUtil.formatAll(sender, myselfMessage));
                return;
            }

            if (cmdSettings.getFPlayer().getIgnoreList().contains(fReceiver.getUuid())) {
                sendMessage(commandSender, getModule() + ".you-ignore");
                return;
            }

            if (fReceiver.getIgnoreList().contains(sender.getUniqueId())) {
                sendMessage(commandSender, getModule() + ".he-ignore");
                return;
            }
        }

        String getMessage = locale.getVaultString(fReceiver.getPlayer(), this + ".get");
        getMessage = MessageUtil.formatPlayerString(cmdSettings.getSender(), getMessage);

        sendGlobalMessage(new ArrayList<>(List.of(fReceiver.getPlayer())), cmdSettings.getSender(),
                cmdSettings.getItemStack(), getMessage, message, true);


        String sendMessage = locale.getVaultString(cmdSettings.getSender(), this + ".send");
        sendMessage = MessageUtil.formatPlayerString(fReceiver.getPlayer(), sendMessage);

        ArrayList<Player> sender = new ArrayList<>();
        if (!cmdSettings.isConsole()) {
            sender.add(cmdSettings.getSender());
            fReceiver.setLastWriter(cmdSettings.getSender());
            cmdSettings.getFPlayer().setLastWriter(fReceiver.getPlayer());
        }

        sendGlobalMessage(sender, fReceiver.getPlayer(),
                cmdSettings.getItemStack(), sendMessage, message, true);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {

        tabCompleteClear();
        switch (args.length) {
            case 1 -> isOnlinePlayer(args[0]);
            case 2 -> isTabCompleteMessage(commandSender, args[1], "message");
        }

        return getSortedTabComplete();
    }
}