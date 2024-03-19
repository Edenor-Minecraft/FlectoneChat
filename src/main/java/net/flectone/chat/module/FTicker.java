package net.flectone.chat.module;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.flectone.chat.FlectoneChat;
import net.flectone.chat.manager.FPlayerManager;
import net.flectone.chat.model.file.FConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class FTicker implements FAction, ScheduledTask, Runnable {

    protected long delay = 1L;
    protected long period;

    @Getter
    private FModule module;

    protected final FConfiguration config;
    protected final FConfiguration locale;
    protected final FPlayerManager playerManager;

    public FTicker(FModule module) {
        this.module = module;

        FlectoneChat plugin = FlectoneChat.getPlugin();
        config = plugin.getFileManager().getConfig();
        locale = plugin.getFileManager().getLocale();
        playerManager = plugin.getPlayerManager();
    }

    @Override
    public void run() {
    }

    public void runTaskTimer() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(FlectoneChat.getPlugin(), v -> this.run(), delay, period);
    }

    public void runTaskTimer(Location location) {
        Bukkit.getRegionScheduler().runAtFixedRate(FlectoneChat.getPlugin(), location, v -> this.run(), delay, period);
    }

    @Override
    public @NotNull CancelledState cancel(){
        return null;
    }

    @Override
    public @NotNull Plugin getOwningPlugin() {
        return FlectoneChat.getPlugin();
    }

    @Override
    public boolean isRepeatingTask() {
        return period > 0;
    }

    @Override
    public @NotNull ExecutionState getExecutionState() {
        return null;
    }

    @Override
    public void init() {

    }
}
