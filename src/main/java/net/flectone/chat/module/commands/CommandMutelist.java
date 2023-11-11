package net.flectone.chat.module.commands;

import net.flectone.chat.builder.FComponentBuilder;
import net.flectone.chat.component.FComponent;
import net.flectone.chat.model.player.Moderation;
import net.flectone.chat.module.FCommand;
import net.flectone.chat.module.FModule;
import net.flectone.chat.util.MessageUtil;
import net.flectone.chat.util.TimeUtil;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandMutelist extends FCommand {

    public CommandMutelist(FModule module, String name) {
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

        int perPage = commands.getInt(getName() + ".per-page");
        if (perPage == 0) {
            throw new RuntimeException("Per-page setting for /" + command + " cannot be zero");
        }

        int mutesCount = database.getCountRow("mutes");
        if (mutesCount == 0) {
            sendMessage(commandSender, this + ".empty");
            return;
        }

        int lastPage = (int) Math.ceil((double) mutesCount / perPage);
        if (args.length != 0 &&
                (!StringUtils.isNumeric(args[0]) || Integer.parseInt(args[0]) < 1 || Integer.parseInt(args[0]) > lastPage)) {
            sendMessage(commandSender, this + ".page-not-exist");
            return;
        }

        CmdSettings cmdSettings = new CmdSettings(commandSender, command);

        if (cmdSettings.isHaveCooldown()) {
            cmdSettings.getFPlayer().sendCDMessage(alias);
            return;
        }

        ComponentBuilder componentBuilder = new ComponentBuilder();

        String title = locale.getVaultString(commandSender, this + ".title")
                .replace("<count>", String.valueOf(mutesCount));
        title = MessageUtil.formatAll(cmdSettings.getSender(), title);

        componentBuilder
                .append(FComponent.fromLegacyText(title))
                .append("\n\n");

        String unmuteButton = MessageUtil.formatAll(cmdSettings.getSender(),
                locale.getVaultString(commandSender, this + ".unmute-button"));

        int page = args.length > 0 ? Math.max(1, Integer.parseInt(args[0])) : 1;
        page = Math.min(lastPage, page);

        database.getModerationList("mutes", perPage, (page - 1) * perPage, Moderation.Type.BAN)
                .forEach(dPlayer -> {

                    String playerBanFormat = locale.getVaultString(commandSender, this + ".player-mute")
                            .replace("<unmute>", unmuteButton)
                            .replace("<player>", dPlayer.getPlayerName())
                            .replace("<reason>", dPlayer.getReason())
                            .replace("<time>", TimeUtil.convertTime(cmdSettings.getSender(), dPlayer.getRemainingTime()))
                            .replace("<moderator>", dPlayer.getModeratorName());
                    playerBanFormat = MessageUtil.formatAll(cmdSettings.getSender(), playerBanFormat);

                    String unmuteHover = locale.getVaultString(commandSender, this + ".unmute-hover")
                            .replace("<player>", dPlayer.getPlayerName());
                    unmuteHover = MessageUtil.formatAll(cmdSettings.getSender(), unmuteHover);

                    FComponent textComponent = new FComponent(playerBanFormat)
                            .addHoverText(unmuteHover)
                            .addRunCommand("/unmute " + dPlayer.getPlayerName());

                    componentBuilder
                            .append(textComponent.get(), ComponentBuilder.FormatRetention.NONE)
                            .append("\n\n");
                });

        componentBuilder.append("", ComponentBuilder.FormatRetention.NONE);

        String pageLine = locale.getVaultString(commandSender, this + ".page-line")
                .replace("<page>", String.valueOf(page))
                .replace("<last-page>", String.valueOf(lastPage));

        pageLine = MessageUtil.formatAll(cmdSettings.getSender(), pageLine);

        FComponentBuilder fComponentBuilder = getfComponentBuilder(pageLine, page, cmdSettings);

        componentBuilder.append(fComponentBuilder.build(cmdSettings.getSender(), cmdSettings.getSender()));

        commandSender.spigot().sendMessage(componentBuilder.create());

        if (!cmdSettings.isConsole()) {
            cmdSettings.getFPlayer().playSound(cmdSettings.getSender(), cmdSettings.getSender(), this.toString());
        }
    }

    @NotNull
    private FComponentBuilder getfComponentBuilder(String pageLine, int page, CmdSettings cmdSettings) {
        FComponentBuilder fComponentBuilder = new FComponentBuilder(pageLine);

        AtomicInteger prevNumber = new AtomicInteger(page);

        fComponentBuilder.replace("<prev-page>", (prevBuilder, color) -> {
            String button = locale.getVaultString(cmdSettings.getSender(), this + ".prev-page");
            button = MessageUtil.formatAll(cmdSettings.getSender(), button);

            FComponent fComponent = new FComponent(color + button)
                    .addRunCommand("/mutelist " + prevNumber.decrementAndGet());

            prevBuilder.append(fComponent.get(), ComponentBuilder.FormatRetention.NONE);
        });

        AtomicInteger nextNumber = new AtomicInteger(page);

        fComponentBuilder.replace("<next-page>", (nextBuilder, color) -> {
            String button = locale.getVaultString(cmdSettings.getSender(), this + ".next-page");
            button = MessageUtil.formatAll(cmdSettings.getSender(), button);

            FComponent fComponent = new FComponent(color + button)
                    .addRunCommand("/mutelist " + nextNumber.incrementAndGet());

            nextBuilder.append(fComponent.get(), ComponentBuilder.FormatRetention.NONE);
        });
        return fComponentBuilder;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return tabCompleteClear();
    }
}
