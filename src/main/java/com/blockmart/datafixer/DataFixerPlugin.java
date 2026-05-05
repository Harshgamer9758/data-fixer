package com.blockmart.datafixer;

import com.blockmart.datafixer.commands.FixDataCommand;
import com.blockmart.datafixer.listeners.PlayerJoinListener;
import com.blockmart.datafixer.managers.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class DataFixerPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ThreadPoolExecutor databaseExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.databaseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        this.databaseManager = new DatabaseManager(this, databaseExecutor);
        this.databaseManager.initializeDatabase();

        getCommand("fixdata").setExecutor(new FixDataCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("DataFixer has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        if (databaseExecutor != null) {
            databaseExecutor.shutdownNow();
        }
        getLogger().info("DataFixer has been disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ThreadPoolExecutor getDatabaseExecutor() {
        return databaseExecutor;
    }
}