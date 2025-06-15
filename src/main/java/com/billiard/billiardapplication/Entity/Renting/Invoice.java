package com.billiard.billiardapplication.Entity.Renting;

import java.time.LocalDateTime;

public class Invoice {
    private String invoiceId;
    private int tableNumber;
    private String customerName;
    private String phoneNumber;
    private LocalDateTime rentalDate;
    private String tableType;
    private double amount;
    private String paymentMethod;

    public Invoice(String invoiceId, int tableNumber, String customerName, String phoneNumber,
                   LocalDateTime rentalDate, String tableType, double amount, String paymentMethod) {
        this.invoiceId = invoiceId;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.rentalDate = rentalDate;
        this.tableType = tableType;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public Invoice() {
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
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

    public LocalDateTime getRentalDate() {
        return rentalDate;
    }

    public void setRentalDate(LocalDateTime rentalDate) {
        this.rentalDate = rentalDate;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", tableNumber=" + tableNumber +
                ", customerName='" + customerName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", rentalDate=" + rentalDate +
                ", tableType='" + tableType + '\'' +
                ", amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}