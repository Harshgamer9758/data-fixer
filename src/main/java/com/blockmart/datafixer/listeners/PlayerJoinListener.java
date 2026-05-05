package com.blockmart.datafixer.listeners;

import com.blockmart.datafixer.DataFixerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final DataFixerPlugin plugin;

    public PlayerJoinListener(DataFixerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDatabaseManager().updatePlayerJoinTime(event.getPlayer().getUniqueId())
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to update player join time for " + event.getPlayer().getName() + ": " + ex.getMessage());
                    return null;
                });

        plugin.getDatabaseManager().getPlayerLastJoinTime(event.getPlayer().getUniqueId())
                .thenAccept(lastJoinTime -> {
                    if (lastJoinTime > 0) {
                        plugin.getLogger().info(event.getPlayer().getName() + " last joined at: " + lastJoinTime);
                    } else {
                        plugin.getLogger().info(event.getPlayer().getName() + " is a new player or no join time recorded.");
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to get player last join time for " + event.getPlayer().getName() + ": " + ex.getMessage());
                    return null;
                });
    }
}