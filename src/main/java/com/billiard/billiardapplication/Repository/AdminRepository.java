package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Person.Admin;

public interface AdminRepository {

    Admin findByUsernamePassword(String username, String password);

    void login();

    void logout();
}
