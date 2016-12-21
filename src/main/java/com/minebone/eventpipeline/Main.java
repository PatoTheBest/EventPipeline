package com.minebone.eventpipeline;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public Main() {
        EventPipeline.setBasePlugin(this);
    }

    @Override
    public void onEnable() {
        EventPipeline.getPipeline(PlayerJoinEvent.class)
                .addFirst("block_join_message", event -> event.setJoinMessage(null))
                .addBefore("block_join_message", "send_greeting", event -> {
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Hello!");
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Be sure to check out our website");
                });

        EventPipeline.getPipeline(PlayerGameModeChangeEvent.class)
                .addFirst("send_message", event -> event.getPlayer().sendMessage(ChatColor.RED + "You changed gamemode"));


        EventPipeline.getPipeline(PlayerCommandPreprocessEvent.class)
                .addFirst("block_plugin_commands", event -> {
                    if(event.getMessage().split(" ")[0].contains(":")) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Executing commands with plugin:comando is blocked!");
                    }
                })
                .addAfter("block_plugin_commands", "inform_command", event -> event.getPlayer().sendMessage("Comando: " + event.getMessage()));
    }

}
