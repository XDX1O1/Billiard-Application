package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Renting.Renting;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Repository.TableRepositoryImpl;

import java.time.Duration;
import java.util.Optional;

public class RentingServiceImpl implements RentingService {

    private TableRepositoryImpl tableRepository;

    public RentingServiceImpl(TableRepositoryImpl tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Override
    public Renting startRental(int tableNumber, String customerName, String phoneNumber, long durationMinutes) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();

        if (!table.isAvailable()) {
            throw new RuntimeException("Table is already occupied: " + tableNumber);
        }

        // Convert minutes to seconds for rentTable method
        long durationSeconds = durationMinutes * 60;
        table.rentTable(customerName, phoneNumber, durationSeconds);

        // Update in database
        tableRepository.update(table);

        return table.getRent();
    }

    @Override
    public void stopRental(int tableNumber) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();

        if (table.isAvailable()) {
            throw new RuntimeException("Table is not currently rented: " + tableNumber);
        }

        try {
            // Stop the rental session
            table.getRent().stopSession();

            // Release the table
            table.removeRent();
            table.setAvailable(true);

            // Update in database
            tableRepository.update(table);

        } catch (InterruptedException e) {
            throw new RuntimeException("Error stopping rental session", e);
        }
    }

    @Override
    public void extendRental(int tableNumber, long additionalMinutes) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();

        if (table.isAvailable() || table.getRent() == null) {
            throw new RuntimeException("Table is not currently rented: " + tableNumber);
        }

        // Convert minutes to hours for extend method
        long additionalHours = additionalMinutes / 60;
        if (additionalMinutes % 60 > 0) {
            additionalHours++; // Round up to next hour
        }

        table.getRent().extendSession(additionalHours);

        // Update in database
        tableRepository.update(table);
    }

    @Override
    public Duration getRemainingTime(int tableNumber) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();

        if (table.isAvailable() || table.getRent() == null) {
            return Duration.ZERO;
        }

        return table.getRent().getRemainingTime();
    }

    @Override
    public float calculateCurrentCost(int tableNumber) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();

        if (table.isAvailable() || table.getRent() == null) {
            return 0.0f;
        }

        return table.getRent().calculateCost();
    }

    @Override
    public Renting getRentalDetails(int tableNumber) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableNumber);
        }

        Table table = tableOpt.get();
        return table.getRent(); // Can be null if not rented
    }

    @Override
    public boolean hasActiveRental(int tableNumber) {
        Optional<Table> tableOpt = tableRepository.findByTableNumber(tableNumber);

        if (tableOpt.isEmpty()) {
            return false;
        }

        Table table = tableOpt.get();
        return !table.isAvailable() && table.getRent() != null;
    }
}