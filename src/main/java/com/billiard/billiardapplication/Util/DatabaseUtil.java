package com.billiard.billiardapplication.Util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseUtil {

    private static HikariDataSource hikariDataSource;

    static {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://localhost:3306/billiard_test");
        config.setUsername("root");
        config.setPassword("plm987!");

        // pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(60_000);
        config.setIdleTimeout(60 * 60 * 1000);

        hikariDataSource = new HikariDataSource(config);
    }

    public static HikariDataSource getHikariDataSource() {
        return hikariDataSource;
    }

    public static void closeAllConnections() {
        try {
            System.out.println("Performing database cleanup...");

            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                System.out.println("Closing HikariCP connection pool...");
                System.out.println("Active connections: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                System.out.println("Idle connections: " + hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                System.out.println("Total connections: " + hikariDataSource.getHikariPoolMXBean().getTotalConnections());

                // Close the HikariCP pool
                hikariDataSource.close();
                System.out.println("HikariCP connection pool closed successfully");
            } else {
                System.out.println("HikariCP connection pool is already closed or null");
            }

            System.out.println("Database cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during database cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void printPoolStatus() {
        try {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                System.out.println("=== HikariCP Pool Status ===");
                System.out.println("Active connections: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                System.out.println("Idle connections: " + hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                System.out.println("Total connections: " + hikariDataSource.getHikariPoolMXBean().getTotalConnections());
                System.out.println("Threads awaiting connection: " + hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                System.out.println("============================");
            } else {
                System.out.println("HikariCP pool is closed or null");
            }
        } catch (Exception e) {
            System.err.println("Error getting pool status: " + e.getMessage());
        }
    }
}
