package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Renting.Invoice;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceRepositoryImpl implements InvoiceRepository {
    private HikariDataSource dataSource;

    public InvoiceRepositoryImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Invoice invoice) {
        // First check if invoice already exists
        if (findById(invoice.getInvoiceId()).isPresent()) {
            System.out.println("Invoice already exists, skipping save: " + invoice.getInvoiceId());
            return;
        }

        String sql = """
            INSERT INTO invoices (invoice_id, table_number, customer_name, phone_number, 
                                rental_date, table_type, amount, payment_method) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, invoice.getInvoiceId());
            stmt.setInt(2, invoice.getTableNumber());
            stmt.setString(3, invoice.getCustomerName());
            stmt.setString(4, invoice.getPhoneNumber());
            stmt.setTimestamp(5, Timestamp.valueOf(invoice.getRentalDate()));
            stmt.setString(6, invoice.getTableType());
            stmt.setDouble(7, invoice.getAmount());
            stmt.setString(8, invoice.getPaymentMethod());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Invoice saved successfully: " + invoice.getInvoiceId());
            } else {
                System.out.println("No rows affected when saving invoice: " + invoice.getInvoiceId());
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getMessage().contains("Duplicate entry")) {
                // MySQL duplicate key error
                System.out.println("Duplicate invoice detected, skipping: " + invoice.getInvoiceId());
                return;
            }
            System.err.println("Error saving invoice: " + e.getMessage());
            throw new RuntimeException("Error saving invoice: " + invoice.getInvoiceId(), e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        List<Invoice> invoices = new ArrayList<>();
        // Use DISTINCT to prevent duplicates and add better ordering
        String sql = """
            SELECT DISTINCT invoice_id, table_number, customer_name, phone_number, 
                   rental_date, table_type, amount, payment_method 
            FROM invoices 
            ORDER BY rental_date DESC, invoice_id DESC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Invoice invoice = mapResultSetToInvoice(rs);
                invoices.add(invoice);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching invoices: " + e.getMessage());
            throw new RuntimeException("Error fetching all invoices", e);
        }

        return invoices;
    }

    @Override
    public Optional<Invoice> findById(String invoiceId) {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, invoiceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInvoice(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching invoice by ID: " + e.getMessage());
            throw new RuntimeException("Error fetching invoice by ID: " + invoiceId, e);
        }

        return Optional.empty();
    }

    @Override
    public List<Invoice> findByTableNumber(int tableNumber) {
        List<Invoice> invoices = new ArrayList<>();
        String sql = """
            SELECT DISTINCT invoice_id, table_number, customer_name, phone_number, 
                   rental_date, table_type, amount, payment_method 
            FROM invoices 
            WHERE table_number = ? 
            ORDER BY rental_date DESC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tableNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching invoices by table number: " + e.getMessage());
            throw new RuntimeException("Error fetching invoices by table number: " + tableNumber, e);
        }

        return invoices;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(rs.getString("invoice_id"));
        invoice.setTableNumber(rs.getInt("table_number"));
        invoice.setCustomerName(rs.getString("customer_name"));
        invoice.setPhoneNumber(rs.getString("phone_number"));

        Timestamp timestamp = rs.getTimestamp("rental_date");
        if (timestamp != null) {
            invoice.setRentalDate(timestamp.toLocalDateTime());
        }

        invoice.setTableType(rs.getString("table_type"));
        invoice.setAmount(rs.getDouble("amount"));
        invoice.setPaymentMethod(rs.getString("payment_method"));

        return invoice;
    }

    public void initializeInvoicesTable() {
        String createTableSql = """
        CREATE TABLE IF NOT EXISTS invoices (
            invoice_id VARCHAR(50) PRIMARY KEY,
            table_number INT NOT NULL,
            customer_name VARCHAR(100) NOT NULL,
            phone_number VARCHAR(20) NOT NULL,
            rental_date TIMESTAMP NOT NULL,
            table_type VARCHAR(20) NOT NULL,
            amount DECIMAL(10,2) NOT NULL,
            payment_method VARCHAR(50) NOT NULL,
            FOREIGN KEY (table_number) REFERENCES tables(table_number),
            UNIQUE KEY unique_invoice (invoice_id),
            INDEX idx_rental_date (rental_date),
            INDEX idx_table_number (table_number)
        )
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSql)) {

            stmt.executeUpdate();
            System.out.println("Invoices table created or already exists");

        } catch (SQLException e) {
            System.err.println("Error creating invoices table: " + e.getMessage());
            throw new RuntimeException("Error creating invoices table", e);
        }
    }

    public void removeDuplicateInvoices() {
        String sql = """
            DELETE i1 FROM invoices i1
            INNER JOIN invoices i2 
            WHERE i1.invoice_id = i2.invoice_id 
            AND i1.rental_date < i2.rental_date
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                System.out.println("Removed " + deletedRows + " duplicate invoices");
            }

        } catch (SQLException e) {
            System.err.println("Error removing duplicate invoices: " + e.getMessage());
            throw new RuntimeException("Error removing duplicate invoices", e);
        }
    }
}