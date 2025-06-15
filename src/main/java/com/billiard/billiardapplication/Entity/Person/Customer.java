package com.billiard.billiardapplication.Entity.Person;

public class Customer {
    private Integer customerId;
    private String customerName;
    private String phoneNumber;
    private static Integer customerCount = 0; // Initialize to 0 instead of null

    public Customer(String customerName, String phoneNumber) {
        // Increment the static counter first
        customerCount++;
        // Then assign the incremented value to this customer's ID
        this.customerId = customerCount;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public static Integer getCustomerCount() {
        return customerCount;
    }

    public static void setCustomerCount(Integer customerCount) {
        Customer.customerCount = customerCount;
    }
}