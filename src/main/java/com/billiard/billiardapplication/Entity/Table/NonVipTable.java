package com.billiard.billiardapplication.Entity.Table;

public class NonVipTable extends Table {
    public NonVipTable(int tableNumber, float pricePerHour) {
        super(tableNumber, pricePerHour);
    }

    @Override
    public String toString() {
        return "Non-VIP Table";
    }
}
