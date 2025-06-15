package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Person.Admin;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminRepositoryImpl implements AdminRepository {

    private HikariDataSource dataSource;
    private Admin currentLoggedInAdmin;

    public AdminRepositoryImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Admin findByUsernamePassword(String username, String password) {
        String sql = "SELECT username, password FROM admin WHERE username = ? AND password = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin(rs.getString("username"), rs.getString("password"));
                    return admin;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding admin", e);
        }

        return null;
    }

    @Override
    public void login() {
        if (currentLoggedInAdmin != null) {
            currentLoggedInAdmin.setLoggedIn(true);

            System.out.println("Admin " + currentLoggedInAdmin.getUsername() + " logged in successfully");
        } else {
            throw new RuntimeException("No admin set for login. Please authenticate first.");
        }
    }

    public void logout() {
        if (currentLoggedInAdmin != null && currentLoggedInAdmin.isLoggedIn()) {
            currentLoggedInAdmin.setLoggedIn(false);

            System.out.println("Admin " + currentLoggedInAdmin.getUsername() + " logged out successfully");
            currentLoggedInAdmin = null;
        } else {
            System.out.println("No admin is currently logged in");
        }
    }

    public boolean authenticate(String username, String password) {
        Admin admin = findByUsernamePassword(username, password);
        if (admin != null) {
            this.currentLoggedInAdmin = admin;
            return true;
        }
        return false;
    }

    public Admin getCurrentLoggedInAdmin() {
        return currentLoggedInAdmin;
    }

    public boolean isAnyAdminLoggedIn() {
        return currentLoggedInAdmin != null && currentLoggedInAdmin.isLoggedIn();
    }
}
