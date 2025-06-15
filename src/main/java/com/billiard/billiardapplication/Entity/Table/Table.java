package com.billiard.billiardapplication.Entity.Table;

import com.billiard.billiardapplication.Entity.Person.Customer;
import com.billiard.billiardapplication.Entity.Renting.Renting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

abstract public class Table {
    private int tableNumber;
    private boolean isAvailable;
    private Renting rentDetail;
    private static int occupiedTable;
    private float pricePerHour;

    public Table(int tableNumber, float pricePerHour) {
        this.tableNumber = tableNumber;
        this.pricePerHour = pricePerHour;
        this.isAvailable = true;
        this.rentDetail = null;
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

    public Renting getRent() {
        return this.rentDetail;
    }

    public static int getOccupiedTable() {
        return occupiedTable;
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

    public void setAvailable(boolean available) {
        if (this.isAvailable && !available) {
            occupiedTable++;
        } else if (!this.isAvailable && available) {
            occupiedTable--;
        }
        this.isAvailable = available;
    }
}