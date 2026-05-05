package com.blockmart.datafixer.managers;

import com.blockmart.datafixer.DataFixerPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

public class DatabaseManager {

    private final DataFixerPlugin plugin;
    private final HikariDataSource dataSource;
    private final ThreadPoolExecutor databaseExecutor;

    public DatabaseManager(DataFixerPlugin plugin, ThreadPoolExecutor databaseExecutor) {
        this.plugin = plugin;
        this.databaseExecutor = databaseExecutor;
        this.dataSource = setupDataSource();
    }

    private HikariDataSource setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("DataFixer-HikariPool");
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.max-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("database.min-idle", 2));
        config.setMaxLifetime(plugin.getConfig().getLong("database.max-lifetime", 1800000L)); // 30 mins
        config.setConnectionTimeout(plugin.getConfig().getLong("database.connection-timeout", 5000L)); // 5 secs
        config.setIdleTimeout(plugin.getConfig().getLong("database.idle-timeout", 600000L)); // 10 mins

        String databasePath = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite-filename", "data.db")).getAbsolutePath();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useLegacyDatetimeCode", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");

        return new HikariDataSource(config);
    }

    public void initializeDatabase() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS player_data ( " +
                                 "uuid VARCHAR(36) PRIMARY KEY, " +
                                 "last_join_time INTEGER " +
                                 ");")) {
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error initializing database: ", e);
            }
        }, databaseExecutor);
    }

    public CompletableFuture<Void> updatePlayerJoinTime(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO player_data (uuid, last_join_time) VALUES (?, ?) " +
                                 "ON CONFLICT(uuid) DO UPDATE SET last_join_time = excluded.last_join_time;"
                 )) {
                stmt.setString(1, playerUuid.toString());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player join time for " + playerUuid, e);
            }
        }, databaseExecutor);
    }

    public CompletableFuture<Long> getPlayerLastJoinTime(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT last_join_time FROM player_data WHERE uuid = ?;"
                 )) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("last_join_time");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting player last join time for " + playerUuid, e);
            }
            return 0L; // Return 0 if not found or an error occurs
        }, databaseExecutor);
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}