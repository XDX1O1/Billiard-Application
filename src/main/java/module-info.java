module com.billiard.billiardapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

//    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.zaxxer.hikari;
    requires java.sql;

    opens com.billiard.billiardapplication.Entity.Renting to javafx.base;
    opens com.billiard.billiardapplication to javafx.fxml;
    opens com.billiard.billiardapplication.Controller to javafx.fxml;
    opens com.billiard.billiardapplication.Repository to javafx.fxml;
    opens com.billiard.billiardapplication.Service to javafx.fxml;
    exports com.billiard.billiardapplication;
    exports com.billiard.billiardapplication.Controller;
    exports com.billiard.billiardapplication.Repository;
    exports com.billiard.billiardapplication.Service;
}