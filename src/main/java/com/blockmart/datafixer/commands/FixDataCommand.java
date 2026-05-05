package com.blockmart.datafixer.commands;

import com.blockmart.datafixer.DataFixerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FixDataCommand implements CommandExecutor {

    private final DataFixerPlugin plugin;

    public FixDataCommand(DataFixerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage(ChatColor.GOLD + "Attempting to fix your data...");

        plugin.getDatabaseManager().updatePlayerJoinTime(player.getUniqueId())
                .thenRun(() -> player.sendMessage(ChatColor.GREEN + "Your last join time has been updated!"))
                .exceptionally(ex -> {
                    player.sendMessage(ChatColor.RED + "Failed to update your join time: " + ex.getMessage());
                    plugin.getLogger().severe("Failed to update data for " + player.getName() + ": " + ex.getMessage());
                    return null;
                });

        return true;
    }
}