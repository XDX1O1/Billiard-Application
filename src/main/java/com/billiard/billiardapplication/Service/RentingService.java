package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Renting.Renting;

import java.time.Duration;

public interface RentingService {

    Renting startRental(int tableNumber, String customerName, String phoneNumber, long durationMinutes);

    void stopRental(int tableNumber);

    void extendRental(int tableNumber, long additionalMinutes);

    Duration getRemainingTime(int tableNumber);

    float calculateCurrentCost(int tableNumber);

    Renting getRentalDetails(int tableNumber);

    boolean hasActiveRental(int tableNumber);
}
