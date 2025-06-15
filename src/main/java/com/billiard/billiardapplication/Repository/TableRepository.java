package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Table.Table;

import java.util.List;
import java.util.Optional;

public interface TableRepository {
    List<Table> findAll();

    Optional<Table> findByTableNumber(int tableNumber);

    void save(Table table);

    void update(Table table);

    List<Table> findByAvailability(boolean isAvailable);

    List<Table> findByType(Class<? extends Table> tableType);
}
