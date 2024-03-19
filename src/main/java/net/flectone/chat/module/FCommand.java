package net.flectone.chat.module;

import lombok.Getter;
import lombok.Setter;
import net.flectone.chat.FlectoneChat;
import net.flectone.chat.builder.MessageBuilder;
import net.flectone.chat.database.sqlite.Database;
import net.flectone.chat.manager.FModuleManager;
import net.flectone.chat.manager.FPlayerManager;
import net.flectone.chat.model.file.FConfiguration;
import net.flectone.chat.model.player.FPlayer;
import net.flectone.chat.model.player.Settings;
import net.flectone.chat.model.sound.FSound;
import net.flectone.chat.module.commands.CommandSpy;
import net.flectone.chat.module.integrations.IntegrationsModule;
import net.flectone.chat.module.sounds.SoundsModule;
import net.flectone.chat.util.CommandsUtil;
import net.flectone.chat.util.MessageUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public abstract class FCommand implements CommandExecutor, TabCompleter, FAction {

    private static final String PLUGIN_NAME = "flectonechat";

    private final List<String> TAB_COMPLETE = new ArrayList<>();
    private final static List<String> TIME_FORMATS = List.of("s", "m", "h", "d", "w", "y");

    private final FModule module;
    private final String name;
    private Command command;

    @Setter
    private boolean isSilent;

    protected final FlectoneChat plugin;
    protected final FPlayerManager playerManager;
    protected final FModuleManager moduleManager;
    protected final Database database;
    protected final FConfiguration locale;
    protected final FConfiguration config;
    protected final FConfiguration commands;

    public FCommand(FModule module, String name) {
        this.module = module;
        this.name = name;

        plugin = FlectoneChat.getPlugin();
        playerManager = plugin.getPlayerManager();
        moduleManager = plugin.getModuleManager();
        database = plugin.getDatabase();
        locale = plugin.getFileManager().getLocale();
        config = plugin.getFileManager().getConfig();
        commands = plugin.getFileManager().getCommands();
    }

    public boolean isEnabled() {
        return commands.getBoolean(name + ".enable");
    }

    public void register() {
        List<String> aliases = commands.getStringList(name + ".aliases");
        PluginCommand pluginCommand = CommandsUtil.createCommand(FlectoneChat.getPlugin(), name, aliases);
        CommandsUtil.getCommandMap().register(PLUGIN_NAME, pluginCommand);

        pluginCommand.setPermission(PLUGIN_NAME + "." + module + "." + name);
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);

        this.command = pluginCommand;
    }

    @Override
    public String toString() {
        return module != null
                ? module + "." + name
                : name;
    }

    @NotNull
    public String getPermission() {
        return module != null
                ? module.getPermission() + "." + name
                : name;
    }

    @Override
    public abstract boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args);

    @Nullable
    @Override
    public abstract List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args);

    public CmdSettings processCommand(@NotNull CommandSender commandSender, @NotNull Command command) {
        return new CmdSettings(commandSender, command);
    }

    public List<String> getFeatures() {
        return commands.getStringList(name + ".features");
    }

    public void sendUsageMessage(@NotNull CommandSender commandSender, @NotNull String alias) {
        String message = locale.getVaultString(commandSender, this + ".usage");
        message = message.replace("<command>", alias);

        Player player = commandSender instanceof Player sender ? sender : null;
        message = MessageUtil.formatAll(player, message);

        commandSender.sendMessage(message);
        sendToSpy(player, new ArrayList<>(), CommandSpy.Type.USAGE, message);
    }

    public void sendErrorMessage(@NotNull CommandSender commandSender, @NotNull String string) {
        String message = locale.getVaultString(commandSender, string);
        message = MessageUtil.formatAll(null, message);

        commandSender.sendMessage(message);
        sendToSpy(commandSender, new ArrayList<>(), CommandSpy.Type.ERROR, message);
    }

    public void sendDefaultMessage(@NotNull CommandSender commandSender, @NotNull String string) {
        String message = locale.getVaultString(commandSender, string);
        message = MessageUtil.formatAll(null, message);

        commandSender.sendMessage(message);
        sendToSpy(commandSender, new ArrayList<>(), CommandSpy.Type.DEFAULT, message);
    }

    public void sendFormattedMessage(@NotNull CommandSender commandSender, @NotNull String string) {
        commandSender.sendMessage(string);
        sendToSpy(commandSender, new ArrayList<>(), CommandSpy.Type.DEFAULT, string);
    }

    public void sendFormattedMessage(@NotNull CommandSender commandSender, @NotNull BaseComponent... components) {
        commandSender.spigot().sendMessage(components);
        sendToSpy(commandSender, new ArrayList<>(), CommandSpy.Type.DEFAULT, "");
    }

    public void sendGlobalMessage(@Nullable Player player, @Nullable ItemStack itemStack, @NotNull String format,
                                  @NotNull String message, boolean isClickable) {

        sendGlobalMessage(getRecipientsList(player), player, itemStack, format, message, isClickable);
    }

    public void sendGlobalMessage(@NotNull Collection<Player> recipients, @Nullable Player player,
                                  @Nullable ItemStack itemStack, @NotNull String format,
                                  @NotNull String message, boolean isClickable) {

        if (player != null) {
            message = IntegrationsModule.interactiveChatMark(message, player.getUniqueId());
        }

        if (!isSilent) {
            FPlayer.sendToConsole(format
                    .replace("<message>", message)
                    .replace("<player>", player != null ? player.getName() : "CONSOLE"));
            sendToSpy(player, recipients, CommandSpy.Type.DEFAULT, message);
        }

        @NotNull String finalMessage = message;
        MessageBuilder messageBuilder = new MessageBuilder(player, itemStack, finalMessage, getFeatures(), true);
        recipients.parallelStream().forEach(recipient -> {
            recipient.sendMessage(messageBuilder.buildFormat(player, recipient, format, isClickable));

            FModule fModule = moduleManager.get(SoundsModule.class);
            if (fModule instanceof SoundsModule soundsModule) {
                soundsModule.play(new FSound(player, recipient, this.toString()));
            }
        });
    }

    private void sendToSpy(@Nullable CommandSender commandSender, @NotNull Collection<Player> recipients,
                           @NotNull CommandSpy.Type type, @NotNull String message) {
        if (!(commandSender instanceof Player player)) return;
        CommandSpy.send(player, command.getName(), recipients, type, message);
    }

    @NotNull
    public Collection<Player> getRecipientsList(@Nullable Player player) {
        return getRecipientsList(player, (Collection<Player>) Bukkit.getOnlinePlayers());
    }

    @NotNull
    public Collection<Player> getRecipientsList(@Nullable Player sender, Collection<Player> playerSet) {
        int range = commands.getInt(name + ".range");

        if (range != 0 && sender != null) {

            Collection<Player> finalPlayerSet = playerSet;
            playerSet = sender.getNearbyEntities(range, range, range).parallelStream()
                    .filter(entity -> entity instanceof Player recipient && finalPlayerSet.contains(recipient))
                    .map(entity -> (Player) entity)
                    .collect(Collectors.toSet());

            if (!playerSet.contains(sender)) playerSet.add(sender);
        }

        playerSet = playerSet.parallelStream()
                .filter(player -> {
                    FPlayer fPlayer = playerManager.get(player);
                    if (fPlayer == null) return true;

                    Settings settings = fPlayer.getSettings();

                    if (settings != null) {

                        Settings.Type type = Settings.Type.fromString("enable_command_" + name);
                        if (type != null) {
                            Object object = settings.getValue(type);
                            if (object != null && String.valueOf(object).equals("-1")) return false;
                        }
                    }

                    return sender == null || !fPlayer.getIgnoreList().contains(sender.getUniqueId());
                })
                .collect(Collectors.toSet());

        return playerSet;
    }
    public List<String> isStartsWith(@NotNull String arg, @NotNull String string, List<String> inlist) {
        if (string.toLowerCase().startsWith(arg.toLowerCase())
                || arg.replace(" ", "").isEmpty()) {
            if (inlist.contains(string)) return inlist;
            inlist.add(string);
        }
        return inlist;
    }

    public List<String> isFileKey(@NotNull FConfiguration file, @NotNull String arg, List<String> inlist) {
        file.getKeys(true).parallelStream()
                .filter(key -> !file.getString(key).contains("root='FConfiguration'"))
                .forEachOrdered(key -> isStartsWith(arg, key, inlist));

        return inlist;
    }

    public List<String> isConfigModePlayer(@NotNull String arg, List<String> inlist) {
        switch (commands.getInt("command." + name + ".tab-complete-mode")) {
            case 0 -> isOfflinePlayer(arg, inlist);
            case 1 -> isOnlinePlayer(arg, inlist);
        }

        return inlist;
    }

    public List<String> isOfflinePlayer(@NotNull String arg, List<String> inlist) {
        playerManager.getOfflinePlayers()
                .parallelStream()
                .forEachOrdered(offlinePlayer -> isStartsWith(arg, offlinePlayer, inlist));

        return inlist;
    }

    public List<String> isOnlinePlayer(@NotNull String arg, List<String> inlist) {
        Bukkit.getOnlinePlayers().parallelStream()
                .filter(player -> !IntegrationsModule.isVanished(player))
                .forEach(player -> isStartsWith(arg, player.getName(), inlist));

        return inlist;
    }

    public List<String> isFormatString(@NotNull String arg, List<String> inlist) {
        TIME_FORMATS.forEach(format -> {
            if (!arg.isEmpty() && StringUtils.isNumeric(arg.substring(arg.length() - 1))) {
                isStartsWith(arg, arg + format, inlist);
                return;
            }

            isDigitInArray(arg, format, 1, 10, inlist);
        });

        return inlist;
    }
    public List<String> isTabCompleteMessage(@NotNull CommandSender commandSender, @NotNull String arg, @NotNull String localeKey, List<String> inlist) {
        return isStartsWith(arg, locale.getVaultString(commandSender, getModule() + ".tab-complete." + localeKey), inlist);
    }public List<String> isDigitInArray(@NotNull String arg, String string, int start, int end, List<String> inlist) {
        for (int x = start; x < end; x++) {
            isStartsWith(arg, x + string, inlist);
        }
        return inlist;
    }

    public List<String> tabCompleteClear() {
        return new ArrayList<>();
    }
    public List<String> getSortedTabComplete(List<String> inlist) {
        inlist.removeIf(Objects::isNull);
        Collections.sort(inlist);
        return inlist;
    }

    public boolean isTimeString(@NotNull String string) {
        return TIME_FORMATS.stream().noneMatch(string::contains);
    }

    public int stringToTime(@NotNull String string) {
        if (string.equals("permanent") || string.equals("-1") || string.equals("0")) return -1;
        int time = Integer.parseInt(string.substring(0, string.length() - 1));
        string = string.substring(string.length() - 1);

        switch (string) {
            case "y":
                time *= 4 * 12 + 4;
            case "w":
                time *= 7;
            case "d":
                time *= 24;
            case "h":
                time *= 60;
            case "m":
                time *= 60;
            case "s":
                break;
        }

        return time;
    }

    public void dispatchCommand(@NotNull CommandSender commandSender, @NotNull String command) {
        if (commandSender instanceof Entity entity){
            entity.getScheduler().run(FlectoneChat.getPlugin(), v -> {
                Bukkit.dispatchCommand(commandSender, command);
            }, null);
        }
        else if (commandSender instanceof ConsoleCommandSender) {
            Bukkit.getGlobalRegionScheduler().run(FlectoneChat.getPlugin(), v ->
                    Bukkit.dispatchCommand(commandSender, command));
        }
        else {
            try {
                Bukkit.getGlobalRegionScheduler().run(FlectoneChat.getPlugin(), v ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            } catch (Exception e) {
                FlectoneChat.warning("Failed to dispatch command!\n" + e.getMessage());
            }
        }
    }

    @Getter
    public class CmdSettings {

        private final boolean isConsole;
        private boolean isDisabled = false;
        private Player sender;
        private ItemStack itemStack;
        private FPlayer fPlayer;
        private final CommandSender commandSender;
        private final Command command;
        private final String commandName;

        public CmdSettings(@NotNull CommandSender commandSender, @NotNull Command command) {
            this.commandSender = commandSender;
            this.command = command;
            this.commandName = command.getName();
            this.isConsole = !(commandSender instanceof Player);

            if (!isConsole) {
                this.sender = (Player) commandSender;
                this.fPlayer = playerManager.get(sender);

                this.itemStack = sender.getInventory().getItemInMainHand();

                if (fPlayer != null) {
                    Object bool = fPlayer.getSettings().getSETTINGS_MAP().get(Settings.Type.fromString("enable_command_" + name));
                    this.isDisabled = bool != null && String.valueOf(bool).equals("-1");
                }
            }
        }

        public boolean isHaveCooldown() {
            if (fPlayer == null) return false;
            return fPlayer.isHaveCooldown("commands." + commandName);
        }

        public boolean isMuted() {
            if (fPlayer == null) return false;
            return fPlayer.isMuted();
        }
    }
}
