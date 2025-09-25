module com.lotify.lotify {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires java.management;
    requires com.github.librepdf.openpdf;
    requires java.desktop;
    requires javafx.swing;
    requires javafx.media;

    opens com.lotify.lotify to javafx.fxml;
    exports com.lotify.lotify;
    exports com.DatabaseConnections;
    opens com.DatabaseConnections to javafx.fxml;
    exports com.Controllers;
    opens com.Controllers to javafx.fxml;
}
