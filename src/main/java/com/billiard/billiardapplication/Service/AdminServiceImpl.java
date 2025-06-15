package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Person.Admin;
import com.billiard.billiardapplication.Repository.AdminRepositoryImpl;

import java.time.LocalDateTime;

public class AdminServiceImpl implements AdminService {

    private final AdminRepositoryImpl adminRepository;
    private LocalDateTime loginTime;

    public AdminServiceImpl(AdminRepositoryImpl adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public boolean login(String username, String password) {
        try {
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty()) {
                return false;
            }

            if (adminRepository.isAnyAdminLoggedIn()) {
                System.out.println("An admin is already logged in. Please logout first.");
                return false;
            }

            if (adminRepository.authenticate(username.trim(), password)) {
                adminRepository.login();
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean logout() {
        try {
            if (adminRepository.isAnyAdminLoggedIn()) {
                adminRepository.logout();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Admin getCurrentAdmin() {
        return adminRepository.getCurrentLoggedInAdmin();
    }

    @Override
    public boolean isLoggedIn() {
        return adminRepository.isAnyAdminLoggedIn();
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        try {
            if (username == null || password == null) {
                return false;
            }

            Admin admin = adminRepository.findByUsernamePassword(username.trim(), password);
            return admin != null;
        } catch (Exception e) {
            System.err.println("Credential validation failed: " + e.getMessage());
            return false;
        }
    }

}

