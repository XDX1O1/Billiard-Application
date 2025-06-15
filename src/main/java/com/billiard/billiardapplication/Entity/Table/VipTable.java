package com.billiard.billiardapplication.Entity.Table;

public class VipTable extends Table {
    private float additionalCost;
    private String[] additionalBenefit = {"Ruangan tanpa rokok",
            "Meja lebih presisi dengan lighting khusus",
            "Menyediakan minuman gratis selama durasi sewa",
            "Ruangan lebih privat"};

    public VipTable(int tableNumber, float pricePerHour, float additionalCost) {
        super(tableNumber, pricePerHour);
        this.additionalCost = additionalCost;
    }

    public float getAdditionalCost() {
        return this.additionalCost;
    }

    public String[] getAdditionalBenefit() {
        return this.additionalBenefit;
    }

    @Override
    public String toString() {
        return "VIP Table";
    }

}
