package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Table.Table;

import java.util.List;
import java.util.Optional;

public interface TableService {

    List<Table> getAllTables();

    List<Table> getFilteredTables(String availabilityFilter, String typeFilter);

    Optional<Table> getTableByNumber(int tableNumber);

    boolean rentTable(int tableNumber, String customerName, String phoneNumber, int durationMinutes, String paymentMethod);

    boolean releaseTable(int tableNumber);

    boolean isTableAvailable(int tableNumber);

    int getOccupiedTableCount();

    void setAvailabilityFilter(String filter);

    void setTypeFilter(String filter);

    String getCurrentAvailabilityFilter();

    String getCurrentTypeFilter();
}
