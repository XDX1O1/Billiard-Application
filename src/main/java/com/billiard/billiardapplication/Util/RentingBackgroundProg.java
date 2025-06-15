package com.billiard.billiardapplication.Util;

import com.billiard.billiardapplication.Entity.Renting.Renting;

import java.time.Duration;

public class RentingBackgroundProg extends Thread {

    private final Renting rental;
    private volatile boolean running = true;

    public RentingBackgroundProg(Renting rental) {
        this.rental = rental;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (running && rental.isActive() && rental.getRemainingTime().toSeconds() > 0) {
            try {
                Thread.sleep(1000);
                Duration currentRemaining = rental.getRemainingTime();
                if (currentRemaining.toSeconds() > 0) {
                    Duration newRemaining = currentRemaining.minusSeconds(1);
                    rental.updateRemainingTime(newRemaining);
                    if (newRemaining.toSecondsPart() == 0) {
                        rental.setTotalCost(rental.calculateCost());
                    }
                } else {
                    rental.updateRemainingTime(Duration.ZERO);
                    break;
                }

            } catch (InterruptedException e) {
                running = false;
                break;
            }
        }
        if (rental.getRemainingTime().toSeconds() <= 0) {
            rental.updateRemainingTime(Duration.ZERO);
        }
    }

    public void stopTimer() {
        running = false;
        this.interrupt();
    }
}
