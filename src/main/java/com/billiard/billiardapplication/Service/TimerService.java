package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Repository.TableRepositoryImpl;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

public class TimerService {
    private static TimerService instance;
    private final Map<Integer, TableTimer> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private TableRepositoryImpl tableRepository;

    private TimerService() {
        // Start the timer update thread
        scheduler.scheduleAtFixedRate(this::updateAllTimers, 1, 1, TimeUnit.SECONDS);
    }

    public static synchronized TimerService getInstance() {
        if (instance == null) {
            instance = new TimerService();
        }
        return instance;
    }

    public void setTableRepository(TableRepositoryImpl tableRepository) {
        this.tableRepository = tableRepository;
    }

    public void startTimer(int tableNumber, int remainingSeconds) {
        TableTimer timer = new TableTimer(tableNumber, remainingSeconds, LocalDateTime.now());
        activeTimers.put(tableNumber, timer);
        System.out.println("Started timer for table " + tableNumber + " with " + remainingSeconds + " seconds");
    }

    public void stopTimer(int tableNumber) {
        activeTimers.remove(tableNumber);
        System.out.println("Stopped timer for table " + tableNumber);
    }

    public int getRemainingSeconds(int tableNumber) {
        TableTimer timer = activeTimers.get(tableNumber);
        return timer != null ? timer.getRemainingSeconds() : 0;
    }

    public boolean hasActiveTimer(int tableNumber) {
        return activeTimers.containsKey(tableNumber);
    }

    private void updateAllTimers() {
        List<Integer> expiredTables = new ArrayList<>();

        for (Map.Entry<Integer, TableTimer> entry : activeTimers.entrySet()) {
            int tableNumber = entry.getKey();
            TableTimer timer = entry.getValue();

            if (timer.getRemainingSeconds() <= 0) {
                // Timer expired
                expiredTables.add(tableNumber);
            }
        }

        // Handle expired timers
        for (int tableNumber : expiredTables) {
            activeTimers.remove(tableNumber);
            handleTimerExpired(tableNumber);
        }
    }

    private void handleTimerExpired(int tableNumber) {
        System.out.println("Timer expired for table " + tableNumber);
        if (tableRepository != null) {
            try {
                // Mark table as available in database
                tableRepository.cleanupExpiredTable(tableNumber);
            } catch (Exception e) {
                System.err.println("Error cleaning up expired table " + tableNumber + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        System.out.println("Shutting down TimerService...");

        try {
            // Stop all active timers by clearing the map
            if (activeTimers != null) {
                System.out.println("Stopping " + activeTimers.size() + " active timers...");
                for (Map.Entry<Integer, TableTimer> entry : activeTimers.entrySet()) {
                    System.out.println("Stopped timer for table: " + entry.getKey());
                }
                activeTimers.clear();
                System.out.println("All timers stopped and cleared");
            }

            // Shutdown the scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    // Wait for existing tasks to complete
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                        // Wait a while for tasks to respond to being cancelled
                        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                            System.err.println("Scheduler did not terminate gracefully");
                        }
                    }
                    System.out.println("Scheduler shutdown completed");
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                    System.err.println("Scheduler shutdown interrupted");
                }
            }

            System.out.println("TimerService shutdown completed successfully");

        } catch (Exception e) {
            System.err.println("Error during TimerService shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Inner class to represent a table timer
    private static class TableTimer {
        private final int tableNumber;
        private final LocalDateTime startTime;
        private final int initialSeconds;

        public TableTimer(int tableNumber, int initialSeconds, LocalDateTime startTime) {
            this.tableNumber = tableNumber;
            this.initialSeconds = initialSeconds;
            this.startTime = startTime;
        }

        public int getRemainingSeconds() {
            LocalDateTime now = LocalDateTime.now();
            long elapsedSeconds = Duration.between(startTime, now).getSeconds();
            return Math.max(0, (int) (initialSeconds - elapsedSeconds));
        }
    }
}