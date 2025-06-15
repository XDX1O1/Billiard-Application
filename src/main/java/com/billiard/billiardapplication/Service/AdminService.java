package com.billiard.billiardapplication.Service;

import com.billiard.billiardapplication.Entity.Person.Admin;

public interface AdminService {

    boolean login(String username, String password);

    boolean logout();

    Admin getCurrentAdmin();

    boolean isLoggedIn();

    boolean validateCredentials(String username, String password);

}
