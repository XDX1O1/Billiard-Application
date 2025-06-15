package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Renting.Invoice;
import com.billiard.billiardapplication.Entity.Table.NonVipTable;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.billiard.billiardapplication.Repository.InvoiceRepositoryImpl;
import com.billiard.billiardapplication.Repository.TableRepositoryImpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TableServiceImpl implements TableService {

    private TableRepositoryImpl tableRepository;
    private InvoiceRepositoryImpl invoiceRepository;
    private String currFilterByAvailability = "None";
    private String currFilterByType = "None";

    public TableServiceImpl(TableRepositoryImpl tableRepository, InvoiceRepositoryImpl invoiceRepository) {
        this.tableRepository = tableRepository;
        this.invoiceRepository = invoiceRepository;
        tableRepository.cleanupExpiredRentals();
    }

    @Override
    public List<Table> getAllTables() {
        tableRepository.cleanupExpiredRentals();

        List<Table> tables = tableRepository.findAll();
        boolean anyChanges = false;
        for (Table table : tables) {
            if (!table.isAvailable() && table.getRent() != null) {
                Duration remainingTime = table.getRent().getRemainingTime();
                if (remainingTime.isZero() || remainingTime.isNegative()) {
                    System.out.println("Found expired table " + table.getTableNumber() + " during getAllTables, cleaning up...");
                    table.removeRent();
                    table.setAvailable(true);
                    try {
                        tableRepository.update(table);
                        anyChanges = true;
                        System.out.println("Cleaned up expired table " + table.getTableNumber());
                    } catch (Exception e) {
                        System.err.println("Error cleaning up expired table " + table.getTableNumber() + ": " + e.getMessage());
                    }
                }
            }
        }
        if (anyChanges) {
            tables = tableRepository.findAll();
        }
        tables.sort((table1, table2) -> {
            if (table1.isAvailable() && table2.isAvailable()) {
                return Integer.compare(table1.getTableNumber(), table2.getTableNumber());
            } else if (table1.isAvailable() && !table2.isAvailable()) {
                return 1;
            } else if (!table1.isAvailable() && table2.isAvailable()) {
                return -1;
            } else {
                Duration duration1 = table1.getRent().getRemainingTime();
                Duration duration2 = table2.getRent().getRemainingTime();
                return duration2.compareTo(duration1);
            }
        });

        return tables;
    }

    @Override
    public List<Table> getFilteredTables(String availabilityFilter, String typeFilter) {
        List<Table> tables = getAllTables();
        if (availabilityFilter != null && !"None".equals(availabilityFilter)) {
            if ("Available".equals(availabilityFilter)) {
                tables = tables.stream()
                        .filter(Table::isAvailable)
                        .collect(Collectors.toList());
            } else if ("Unavailable".equals(availabilityFilter)) {
                tables = tables.stream()
                        .filter(table -> !table.isAvailable())
                        .collect(Collectors.toList());
            }
        }
        if (typeFilter != null && !"None".equals(typeFilter)) {
            if ("VIP".equals(typeFilter)) {
                tables = tables.stream()
                        .filter(table -> table instanceof VipTable)
                        .collect(Collectors.toList());
            } else if ("NON_VIP".equals(typeFilter)) {
                tables = tables.stream()
                        .filter(table -> table instanceof NonVipTable)
                        .collect(Collectors.toList());
            }
        }

        return tables;
    }


    @Override
    public Optional<Table> getTableByNumber(int tableNumber) {
        return tableRepository.findByTableNumber(tableNumber);
    }

    @Override
    public boolean rentTable(int tableNumber, String customerName, String phoneNumber, int durationMinutes, String paymentMethod) {
        System.out.println("=== Starting rentTable process ===");
        System.out.println("Table: " + tableNumber + ", Customer: " + customerName + ", Phone: " + phoneNumber);
        System.out.println("Duration: " + durationMinutes + " minutes, Payment: " + paymentMethod);

        try {
            System.out.println("Step 1: Finding table...");
            Optional<Table> tableOpt = getTableByNumber(tableNumber);
            if (tableOpt.isEmpty()) {
                System.err.println("ERROR: Table " + tableNumber + " not found");
                return false;
            }

            Table table = tableOpt.get();
            System.out.println("Found table: " + table.getTableNumber() + ", Available: " + table.isAvailable());

            if (!table.isAvailable()) {
                System.err.println("ERROR: Table " + tableNumber + " is not available");
                return false;
            }
            System.out.println("Step 2: Validating inputs...");
            if (customerName == null || customerName.trim().isEmpty()) {
                System.err.println("ERROR: Customer name is empty");
                return false;
            }
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                System.err.println("ERROR: Phone number is empty");
                return false;
            }
            if (durationMinutes <= 0) {
                System.err.println("ERROR: Duration must be positive: " + durationMinutes);
                return false;
            }
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                System.err.println("ERROR: Payment method is empty");
                return false;
            }
            System.out.println("Step 3: Calculating cost and renting table...");
            long durationInSeconds = durationMinutes * 60L;
            double hourlyRate = table instanceof VipTable ? 25000 : 15000;
            double totalCost = (durationMinutes / 60.0) * hourlyRate;

            System.out.println("Hourly rate: " + hourlyRate + ", Total cost: " + totalCost);
            System.out.println("Duration in seconds: " + durationInSeconds);
            table.rentTable(customerName.trim(), phoneNumber.trim(), durationInSeconds);
            System.out.println("Table rented successfully in memory");
            if (table.getRent() == null) {
                System.err.println("ERROR: Rental was not set properly on table");
                return false;
            }

            System.out.println("Rental verification - Customer: " + table.getRent().getCustomer().getCustomerName());
            System.out.println("Rental verification - Remaining time: " + table.getRent().getRemainingTime().getSeconds() + " seconds");
            System.out.println("Step 4: Updating table in database...");
            try {
                tableRepository.update(table);
                System.out.println("Table updated in database successfully");
                Optional<Table> verifyTable = tableRepository.findByTableNumber(tableNumber);
                if (verifyTable.isPresent()) {
                    Table dbTable = verifyTable.get();
                    System.out.println("DB Verification - Available: " + dbTable.isAvailable());
                    if (!dbTable.isAvailable() && dbTable.getRent() != null) {
                        System.out.println("DB Verification - Customer: " + dbTable.getRent().getCustomer().getCustomerName());
                        System.out.println("DB Verification - Remaining time: " + dbTable.getRent().getRemainingTime().getSeconds() + " seconds");
                    }
                }

            } catch (Exception e) {
                System.err.println("ERROR: Failed to update table in database: " + e.getMessage());
                e.printStackTrace();
                table.removeRent();
                table.setAvailable(true);
                return false;
            }
            System.out.println("Step 5: Creating invoice...");
            try {
                String invoiceId = generateInvoiceId();
                String tableType = table instanceof VipTable ? "VIP" : "NON_VIP";
                LocalDateTime rentalDate = LocalDateTime.now();

                System.out.println("Invoice ID: " + invoiceId + ", Table Type: " + tableType);
                Invoice invoice = new Invoice(
                        invoiceId,
                        tableNumber,
                        customerName.trim(),
                        phoneNumber.trim(),
                        rentalDate,
                        tableType,
                        totalCost,
                        paymentMethod.trim()
                );

                System.out.println("Step 6: Saving invoice to database...");
                invoiceRepository.save(invoice);
                System.out.println("Invoice saved successfully");

            } catch (Exception e) {
                System.err.println("ERROR: Failed to save invoice: " + e.getMessage());
                e.printStackTrace();
                try {
                    table.removeRent();
                    table.setAvailable(true);
                    tableRepository.update(table);
                    System.out.println("Rolled back table rental due to invoice error");
                } catch (Exception rollbackException) {
                    System.err.println("ERROR: Failed to rollback table rental: " + rollbackException.getMessage());
                }
                return false;
            }

            System.out.println("=== rentTable completed successfully ===");
            System.out.println("Table " + tableNumber + " rented to " + customerName +
                    " (Phone: " + phoneNumber + ") for " + durationMinutes +
                    " minutes using " + paymentMethod);
            return true;

        } catch (Exception e) {
            System.err.println("=== UNEXPECTED ERROR in rentTable ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean releaseTable(int tableNumber) {
        System.out.println("=== Starting releaseTable process ===");
        System.out.println("Releasing table: " + tableNumber);

        try {
            Optional<Table> tableOpt = getTableByNumber(tableNumber);
            if (tableOpt.isEmpty()) {
                System.err.println("ERROR: Table " + tableNumber + " not found");
                return false;
            }

            Table table = tableOpt.get();
            System.out.println("Found table: " + table.getTableNumber() + ", Available: " + table.isAvailable());

            if (table.isAvailable()) {
                System.err.println("ERROR: Table " + tableNumber + " is already available");
                return false;
            }
            if (table.getRent() != null && table.getRent().getCustomer() != null) {
                String customerName = table.getRent().getCustomer().getCustomerName();
                Duration remainingTime = table.getRent().getRemainingTime();
                System.out.println("Releasing rental for customer: " + customerName);
                System.out.println("Remaining time: " + remainingTime.toMinutes() + " minutes");
            }
            table.removeRent();
            table.setAvailable(true);
            System.out.println("Table released in memory");
            System.out.println("Updating table in database...");
            tableRepository.update(table);
            System.out.println("Table updated in database successfully");
            Optional<Table> verifyTable = tableRepository.findByTableNumber(tableNumber);
            if (verifyTable.isPresent()) {
                Table dbTable = verifyTable.get();
                System.out.println("DB Verification - Available: " + dbTable.isAvailable());
            }

            System.out.println("=== releaseTable completed successfully ===");
            System.out.println("Table " + tableNumber + " has been released and is now available");
            return true;

        } catch (Exception e) {
            System.err.println("=== UNEXPECTED ERROR in releaseTable ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isTableAvailable(int tableNumber) {
        Optional<Table> tableOpt = getTableByNumber(tableNumber);
        return tableOpt.map(Table::isAvailable).orElse(false);
    }

    @Override
    public int getOccupiedTableCount() {
        return (int) getAllTables().stream()
                .filter(table -> !table.isAvailable())
                .count();
    }

    @Override
    public void setAvailabilityFilter(String filter) {
        if ("Available".equals(filter) || "Unavailable".equals(filter) || "None".equals(filter)) {
            this.currFilterByAvailability = filter;
        } else {
            System.out.println("Filter by " + filter + " is not an option");
        }
    }

    @Override
    public void setTypeFilter(String filter) {
        if ("VIP".equals(filter) || "NON_VIP".equals(filter) || "None".equals(filter)) {
            this.currFilterByType = filter;
        } else {
            System.out.println("Filter by " + filter + " is not an option");
        }
    }

    @Override
    public String getCurrentAvailabilityFilter() {
        return currFilterByAvailability;
    }

    @Override
    public String getCurrentTypeFilter() {
        return currFilterByType;
    }

    public TableRepositoryImpl getTableRepository() {
        return this.tableRepository;
    }

    private String generateInvoiceId() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);
        int randomSuffix = (int) (Math.random() * 90) + 10;
        return "INV-" + timestamp + "-" + randomSuffix;
    }
}