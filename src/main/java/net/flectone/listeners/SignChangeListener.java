package net.flectone.listeners;

import net.flectone.messages.MessageBuilder;
import net.flectone.utils.ObjectUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class SignChangeListener implements Listener {

    @EventHandler
    public void onSignChange(SignChangeEvent event){
        Player player = event.getPlayer();
        String command =  "sign";
        ItemStack itemInHand = player.getItemInUse();

        for(int x = 0; x < event.getLines().length; x++){
            String string = event.getLine(x);

            if(string == null || string.isEmpty()) continue;

            event.setLine(x, ObjectUtil.buildFormattedMessage(player, command, string, itemInHand));
        }
    }
}
