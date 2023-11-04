package net.flectone.chat.module.extra.spit;


import net.flectone.chat.manager.FPlayerManager;
import net.flectone.chat.model.player.FPlayer;
import net.flectone.chat.module.FListener;
import net.flectone.chat.module.FModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static net.flectone.chat.manager.FileManager.config;

public class SpitListener extends FListener {

    public SpitListener(FModule module) {
        super(module);
        init();
    }

    @EventHandler
    public void spitEvent(@NotNull PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Player player = event.getPlayer();
        if (!config.getVaultBoolean(player, getModule() + ".enable")) return;
        if (hasNoPermission(player)) return;

        String configMaterial = config.getVaultString(player, getModule() + ".item");
        if (!item.getType().toString().equalsIgnoreCase(configMaterial)) return;

        FPlayer fPlayer = FPlayerManager.get(player);
        if (fPlayer == null) return;

        if (fPlayer.isMuted()) {
            fPlayer.sendMutedMessage();
            return;
        }

        if (fPlayer.isHaveCooldown(getModule().toString())) {
            fPlayer.sendCDMessage("spit");
            return;
        }

        ((SpitModule) getModule()).spit(player);
    }

    @Override
    public void init() {
        register();
    }
}
