package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Renting.Renting;
import com.billiard.billiardapplication.Entity.Table.NonVipTable;
import com.billiard.billiardapplication.Entity.Table.Table;
import com.billiard.billiardapplication.Entity.Table.VipTable;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableRepositoryImpl implements TableRepository {
    private HikariDataSource dataSource;
    private List<Table> tables;

    public TableRepositoryImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.tables = new ArrayList<>();
    }

    @Override
    public List<Table> findAll() {
        List<Table> tables = new ArrayList<>();
        String sql = "SELECT * FROM tables ORDER BY table_number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Table table = mapResultSetToTable(rs);
                tables.add(table);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all tables", e);
        }
        return tables;
    }

    @Override
    public Optional<Table> findByTableNumber(int tableNumber) {
        String sql = "SELECT * FROM tables WHERE table_number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tableNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTable(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching table by number: " + tableNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public void save(Table table) {
        String sql = "INSERT INTO tables (table_number, table_type, is_available, price_per_hour) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, table.getTableNumber());
            stmt.setString(2, table instanceof VipTable ? "VIP" : "NON_VIP");
            stmt.setBoolean(3, table.isAvailable());
            stmt.setFloat(4, table.getPricePerHour());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving table: " + table.getTableNumber(), e);
        }
    }

    public void update(Table table) {
        String sql = """
                UPDATE tables 
                SET is_available = ?, 
                    customer_name = ?, 
                    phone_number = ?, 
                    rental_start_time = ?, 
                    rental_duration_minutes = ? 
                WHERE table_number = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, table.isAvailable());

            if (table.isAvailable() || table.getRent() == null) {
                // Table is available, clear rental data
                statement.setNull(2, Types.VARCHAR);
                statement.setNull(3, Types.VARCHAR);
                statement.setNull(4, Types.TIMESTAMP);
                statement.setNull(5, Types.BIGINT);
            } else {
                // Table is rented, save rental data
                Renting rent = table.getRent();
                statement.setString(2, rent.getCustomer().getCustomerName());
                statement.setString(3, rent.getCustomer().getPhoneNumber());
                statement.setTimestamp(4, Timestamp.valueOf(rent.getStartTime()));

                // Calculate total duration in minutes
                Duration totalDuration = Duration.between(rent.getStartTime(), rent.getEndTime());
                statement.setLong(5, totalDuration.toMinutes());
            }

            statement.setInt(6, table.getTableNumber());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No table found with number: " + table.getTableNumber());
            }

            System.out.println("Updated table " + table.getTableNumber() +
                    " - Available: " + table.isAvailable());

        } catch (SQLException e) {
            System.err.println("Error updating table: " + e.getMessage());
            throw new RuntimeException("Database error updating table", e);
        }
    }

    @Override
    public List<Table> findByAvailability(boolean isAvailable) {
        List<Table> tables = new ArrayList<>();
        String sql = "SELECT * FROM tables WHERE is_available = ? ORDER BY table_number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isAvailable);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(mapResultSetToTable(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching tables by availability", e);
        }
        return tables;
    }

    @Override
    public List<Table> findByType(Class<? extends Table> tableType) {
        List<Table> tables = new ArrayList<>();
        String typeString = tableType == VipTable.class ? "VIP" : "NON_VIP";
        String sql = "SELECT * FROM tables WHERE table_type = ? ORDER BY table_number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, typeString);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(mapResultSetToTable(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching tables by type", e);
        }
        return tables;
    }

    // Helper method to map ResultSet to Table object
    private Table mapResultSetToTable(ResultSet rs) throws SQLException {
        int tableNumber = rs.getInt("table_number");
        String tableType = rs.getString("table_type");
        boolean isAvailable = rs.getBoolean("is_available");
        float pricePerHour = rs.getFloat("price_per_hour");

        Table table;
        if ("VIP".equals(tableType)) {
            table = new VipTable(tableNumber, pricePerHour, 40000f);
        } else {
            table = new NonVipTable(tableNumber, pricePerHour);
        }

        // If table is marked as available in database, no need to check rental info
        if (isAvailable) {
            table.setAvailable(true);
            return table;
        }

        // If table is not available, check rental information
        String customerName = rs.getString("customer_name");
        String phoneNumber = rs.getString("phone_number");
        Timestamp startTime = rs.getTimestamp("rental_start_time");
        Long durationMinutes = rs.getLong("rental_duration_minutes");

        if (customerName != null && startTime != null && durationMinutes != null) {
            LocalDateTime rentalStartTime = startTime.toLocalDateTime();
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime originalEndTime = rentalStartTime.plusMinutes(durationMinutes);

            // Check if rental has expired
            if (currentTime.isAfter(originalEndTime) || currentTime.equals(originalEndTime)) {
                // Rental has expired, mark table as available and update database
                System.out.println("Table " + tableNumber + " rental has expired, cleaning up...");
                table.setAvailable(true);

                // Clean up the expired rental in database immediately
                cleanupExpiredTable(tableNumber);

                return table;
            } else {
                // Calculate remaining time
                long remainingSeconds = Duration.between(currentTime, originalEndTime).getSeconds();

                // If less than 1 second remaining, consider it expired
                if (remainingSeconds <= 0) {
                    System.out.println("Table " + tableNumber + " has no time remaining, cleaning up...");
                    table.setAvailable(true);
                    cleanupExpiredTable(tableNumber);
                    return table;
                }

                // Only create rental if there's meaningful time remaining (more than 0 seconds)
                try {
                    table.rentTable(customerName, phoneNumber, remainingSeconds);
                    System.out.println("Restored rental for table " + tableNumber +
                            " with " + remainingSeconds + " seconds remaining");
                } catch (Exception e) {
                    System.err.println("Error restoring rental for table " + tableNumber + ": " + e.getMessage());
                    table.setAvailable(true);
                    cleanupExpiredTable(tableNumber);
                }
            }
        } else {
            // No rental information but marked as unavailable - this is inconsistent
            System.out.println("Table " + tableNumber + " marked unavailable but no rental info, marking as available");
            table.setAvailable(true);
            cleanupExpiredTable(tableNumber);
        }

        return table;
    }

    // Helper method to initialize tables if needed
    public void initializeTables(List<Table> initialTables) {
        this.tables = new ArrayList<>(initialTables);
    }

    // Method to initialize tables in database (run once)
    public void initializeTablesInDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            // First, create the table if it doesn't exist
            createTableIfNotExists(conn);

            // Then check if we need to insert initial data
            String checkSql = "SELECT COUNT(*) FROM tables";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {

                rs.next();
                int count = rs.getInt(1);

                if (count == 0) {
                    // Initialize with your table setup
                    insertInitialTables(conn);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking/initializing tables", e);
        }
    }

    // Create the tables table if it doesn't exist
    private void createTableIfNotExists(Connection conn) throws SQLException {
        String createTableSql = """
                    CREATE TABLE IF NOT EXISTS tables (
                        table_number INT PRIMARY KEY,
                        table_type VARCHAR(10) NOT NULL,
                        is_available BOOLEAN NOT NULL DEFAULT TRUE,
                        price_per_hour FLOAT NOT NULL,
                        customer_name VARCHAR(100),
                        phone_number VARCHAR(20),
                        rental_start_time TIMESTAMP NULL,
                        rental_duration_minutes BIGINT
                    )
                """;

        try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.executeUpdate();
        }
    }

    private void insertInitialTables(Connection conn) throws SQLException {
        String sql = "INSERT INTO tables (table_number, table_type, is_available, price_per_hour) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Non-VIP tables 1-6 (assuming price 15000 per hour)
            for (int i = 1; i <= 6; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "NON_VIP");
                stmt.setBoolean(3, true);
                stmt.setFloat(4, 15000f);
                stmt.addBatch();
            }

            // VIP tables 7-12 (assuming price 25000 per hour)
            for (int i = 7; i <= 12; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "VIP");
                stmt.setBoolean(3, true);
                stmt.setFloat(4, 25000f);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    public void cleanupExpiredRentals() {
        String sql = """
                UPDATE tables 
                SET is_available = TRUE, 
                    customer_name = NULL, 
                    phone_number = NULL, 
                    rental_start_time = NULL, 
                    rental_duration_minutes = NULL 
                WHERE is_available = FALSE 
                AND rental_start_time IS NOT NULL 
                AND rental_duration_minutes IS NOT NULL
                AND TIMESTAMPADD(MINUTE, rental_duration_minutes, rental_start_time) <= NOW()
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                System.out.println("Cleaned up " + updatedRows + " expired rentals");
            }

        } catch (SQLException e) {
            System.err.println("Error cleaning up expired rentals: " + e.getMessage());
            throw new RuntimeException("Database error cleaning up expired rentals", e);
        }
    }

    public void cleanupExpiredTable(int tableNumber) {
        String sql = """
                UPDATE tables 
                SET is_available = TRUE, 
                    customer_name = NULL, 
                    phone_number = NULL, 
                    rental_start_time = NULL, 
                    rental_duration_minutes = NULL 
                WHERE table_number = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, tableNumber);
            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("Cleaned up expired table " + tableNumber);
            } else {
                System.out.println("No table found with number " + tableNumber + " to clean up");
            }

        } catch (SQLException e) {
            System.err.println("Error cleaning up expired table " + tableNumber + ": " + e.getMessage());
            throw new RuntimeException("Database error cleaning up expired table", e);
        }
    }

    private void updateExpiredTable(int tableNumber) {
        String sql = """
                UPDATE tables 
                SET is_available = TRUE, 
                    customer_name = NULL, 
                    phone_number = NULL, 
                    rental_start_time = NULL, 
                    rental_duration_minutes = NULL 
                WHERE table_number = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, tableNumber);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating expired table " + tableNumber + ": " + e.getMessage());
        }
    }

    public void updateRemainingTime(int tableNumber, LocalDateTime currentTime, int remainingMinutes) {
        String sql = """
                UPDATE tables 
                SET rental_start_time = ?, 
                    rental_duration_minutes = ? 
                WHERE table_number = ? AND is_available = FALSE
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Calculate new start time so that the end time reflects the remaining time
            LocalDateTime newStartTime = currentTime;
            statement.setTimestamp(1, Timestamp.valueOf(newStartTime));
            statement.setLong(2, remainingMinutes);
            statement.setInt(3, tableNumber);

            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("Updated remaining time for table " + tableNumber +
                        " to " + remainingMinutes + " minutes");
            }

        } catch (SQLException e) {
            System.err.println("Error updating remaining time for table " + tableNumber + ": " + e.getMessage());
            throw new RuntimeException("Database error updating remaining time", e);
        }
    }

    public boolean hasExpiredRentals() {
        String sql = """
                SELECT COUNT(*) 
                FROM tables 
                WHERE is_available = FALSE 
                AND rental_start_time IS NOT NULL 
                AND rental_duration_minutes IS NOT NULL
                AND TIMESTAMPADD(MINUTE, rental_duration_minutes, rental_start_time) <= NOW()
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            rs.next();
            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println("Error checking for expired rentals: " + e.getMessage());
            return false;
        }
    }
}