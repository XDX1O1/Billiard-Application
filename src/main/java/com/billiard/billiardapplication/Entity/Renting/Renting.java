package com.billiard.billiardapplication.Entity.Renting;

import com.billiard.billiardapplication.Entity.Person.Customer;
import com.billiard.billiardapplication.Entity.Table.NonVipTable;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.billiard.billiardapplication.Util.RentingBackgroundProg;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Dictionary;

public class Renting {
    protected Customer customer;
    protected Table table;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    protected volatile Duration remainingTime; // Made volatile for thread safety
    private float totalCost;
    private Dictionary<String, Object> details;
    private RentingBackgroundProg runningTime;
    private boolean isActive;

    public Renting(Table table, Customer customer, LocalDateTime startTime, LocalDateTime endTime) {
        this.customer = customer;
        this.table = table;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = Duration.between(startTime, endTime);
        this.remainingTime = duration;
        this.totalCost = 0;
        this.isActive = true;

        // Start background timer
        runningTime = new RentingBackgroundProg(this);
        runningTime.start();
    }

    public Table getTable() {
        return this.table;
    }

    public Customer getCustomer() {
        return this.customer;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public Duration getDuration() {
        return this.duration;
    }

    public long getDurationInMinutes() {
        return this.duration.toMinutes();
    }

    public Duration getRemainingTime() {
        return remainingTime;
    }

    // Method to update remaining time (called by background thread)
    public synchronized void updateRemainingTime(Duration newRemainingTime) {
        this.remainingTime = newRemainingTime;
    }

    public float getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(float totalCost) {
        this.totalCost = totalCost;
    }

    public boolean isActive() {
        return isActive;
    }

    public float calculateCost() {
        // Calculate cost based on actual time used (original duration - remaining time)
        Duration usedTime = duration.minus(remainingTime);
        double hoursUsed = usedTime.toMinutes() / 60.0;

        if (table instanceof VipTable) {
            VipTable vipTable = (VipTable) table;
            return (float) (vipTable.getAdditionalCost() + (table.getPricePerHour() * hoursUsed));
        } else if (table instanceof NonVipTable) {
            return (float) (table.getPricePerHour() * hoursUsed);
        }
        return 0;
    }

    public void extendSession(long hours) {
        this.endTime = endTime.plusHours(hours);
        this.duration = duration.plusHours(hours);

        synchronized (this) {
            this.remainingTime = remainingTime.plusHours(hours);
        }
    }

    public void stopSession() throws InterruptedException {
        this.isActive = false;

        // Calculate final duration based on actual usage
        LocalDateTime actualEndTime = LocalDateTime.now();
        this.duration = Duration.between(startTime, actualEndTime);
        this.endTime = actualEndTime;

        synchronized (this) {
            this.remainingTime = Duration.ZERO;
        }

        // Stop the background thread
        if (runningTime != null && runningTime.isAlive()) {
            runningTime.interrupt();
            runningTime.join();
        }

        // Calculate final cost
        totalCost = calculateCost();
    }

}
