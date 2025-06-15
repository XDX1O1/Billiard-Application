package com.billiard.billiardapplication.Util;

import com.billiard.billiardapplication.Entity.Renting.Renting;

import java.time.Duration;

public class RentingBackgroundProg extends Thread {

    private Renting rental;
    private volatile boolean running = true;

    public RentingBackgroundProg(Renting rental) {
        this.rental = rental;
        this.setDaemon(true); // Make this a daemon thread
    }

    @Override
    public void run() {
        while (running && rental.isActive() && rental.getRemainingTime().toSeconds() > 0) {
            try {
                Thread.sleep(1000); // Sleep for 1 second

                // Decrement remaining time by 1 second
                Duration currentRemaining = rental.getRemainingTime();
                if (currentRemaining.toSeconds() > 0) {
                    Duration newRemaining = currentRemaining.minusSeconds(1);
                    rental.updateRemainingTime(newRemaining);

                    // Update total cost every minute
                    if (newRemaining.toSecondsPart() == 0) {
                        rental.setTotalCost(rental.calculateCost());
                    }
                } else {
                    // Time is up
                    rental.updateRemainingTime(Duration.ZERO);
                    break;
                }

            } catch (InterruptedException e) {
                // Thread was interrupted, stop running
                running = false;
                break;
            }
        }

        // Ensure remaining time is set to zero when done
        if (rental.getRemainingTime().toSeconds() <= 0) {
            rental.updateRemainingTime(Duration.ZERO);
        }
    }

    public void stopTimer() {
        running = false;
        this.interrupt();
    }
}
