package com.billiard.billiardapplication.Entity.Table;

import com.billiard.billiardapplication.Entity.Person.Customer;
import com.billiard.billiardapplication.Entity.Renting.Renting;

import java.time.Duration;
import java.time.LocalDateTime;

abstract public class Table {
    private static int occupiedTable;
    private final int tableNumber;
    private boolean isAvailable;
    private Renting rentDetail;
    private final float pricePerHour;

    public Table(int tableNumber, float pricePerHour) {
        this.tableNumber = tableNumber;
        this.pricePerHour = pricePerHour;
        this.isAvailable = true;
        this.rentDetail = null;
    }

    public static int getOccupiedTable() {
        return occupiedTable;
    }

    public float getPricePerHour() {
        return this.pricePerHour;
    }

    public int getTableNumber() {
        return this.tableNumber;
    }

    public boolean isAvailable() {
        return this.isAvailable;
    }

    public void setAvailable(boolean available) {
        if (this.isAvailable && !available) {
            occupiedTable++;
        } else if (!this.isAvailable && available) {
            occupiedTable--;
        }
        this.isAvailable = available;
    }

    public Renting getRent() {
        return this.rentDetail;
    }

    public void removeRent() {
        this.rentDetail = null;
    }

    public void rentTable(String customerName, String phoneNumber, long durationSeconds) {
        Customer customer = new Customer(customerName, phoneNumber);
        Duration rentDuration = Duration.ofSeconds(durationSeconds);

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plus(rentDuration);

        Renting r = new Renting(this, customer, startTime, endTime);
        this.isAvailable = false;
        this.rentDetail = r;
        occupiedTable++;
    }
}