package com.billiard.billiardapplication.Repository;

import com.billiard.billiardapplication.Entity.Renting.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    void save(Invoice invoice);

    List<Invoice> findAll();

    Optional<Invoice> findById(String invoiceId);

    List<Invoice> findByTableNumber(int tableNumber);

}
