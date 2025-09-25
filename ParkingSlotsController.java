package com.Controllers;

import com.DatabaseConnections.DatabaseConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class ParkingSlotsController {

    @FXML private AnchorPane contentArea;
    @FXML private Button motorcycleTab;
    @FXML private Button truckTab;
    @FXML private Button suvTab;
    @FXML private Button carTab;

    private Button currentlySelectedTab;
    private static String lastOpenedFXML = null;

    @FXML
    public void initialize() throws IOException {

        if (lastOpenedFXML == null) {
            showMotorcycles(null); // default

        } else {
            loadUI(lastOpenedFXML, getVehicleTypeFromFXML(lastOpenedFXML));
            switch (lastOpenedFXML) {
                case "motorcycles.fxml" -> setActiveTab(motorcycleTab);
                case "cars.fxml" -> setActiveTab(carTab);
                case "suvs.fxml" -> setActiveTab(suvTab);
                case "trucks.fxml" -> setActiveTab(truckTab);
            }
        }
    }

    @FXML
    void showMotorcycles(ActionEvent event) throws IOException {
        loadUI("motorcycles.fxml", "Motorcycle");
        lastOpenedFXML = "motorcycles.fxml";
        highlightSelectedTab(motorcycleTab);
        setActiveTab(motorcycleTab);
    }

    @FXML
    void showTrucks(ActionEvent event) throws IOException {
        loadUI("trucks.fxml", "Truck");
        lastOpenedFXML = "trucks.fxml";
        highlightSelectedTab(truckTab);
        setActiveTab(truckTab);
    }

    @FXML
    void showSUVs(ActionEvent event) throws IOException {
        loadUI("suvs.fxml", "SUV");
        lastOpenedFXML = "suvs.fxml";
        highlightSelectedTab(suvTab);
        setActiveTab(suvTab);
    }

    @FXML
    void showCars(ActionEvent event) throws IOException {
        loadUI("cars.fxml", "Car");
        lastOpenedFXML = "cars.fxml";
        highlightSelectedTab(carTab);
        setActiveTab(carTab);
    }

    private void loadUI(String fxml, String vehicleType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lotify/lotify/" + fxml));
        Node node = loader.load();

        contentArea.getChildren().setAll(node);
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);

        updateSlotColors(node, vehicleType);
    }


    private void setActiveTab(Button selectedTab) {
        if (currentlySelectedTab != null) {
            currentlySelectedTab.getStyleClass().remove("selected-tab");
        }

        selectedTab.getStyleClass().add("selected-tab");
        currentlySelectedTab = selectedTab;
    }

    private void highlightSelectedTab(Button selectedTab) {
        motorcycleTab.getStyleClass().remove("nav-selected");
        truckTab.getStyleClass().remove("nav-selected");
        suvTab.getStyleClass().remove("nav-selected");
        carTab.getStyleClass().remove("nav-selected");

        selectedTab.getStyleClass().add("nav-selected");
    }


    private void updateSlotColors(Node root, String vehicleType) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT slot_id, is_occupied FROM slots WHERE vehicle_type = ?")) {

            stmt.setString(1, vehicleType);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String slotId = rs.getString("slot_id");
                boolean occupied = rs.getBoolean("is_occupied");

                Button slotButton = findButton(root, slotId);
                if (slotButton != null) {
                    slotButton.getStyleClass().removeAll("slot-vacant", "slot-occupied");

                    if (occupied) {
                        slotButton.getStyleClass().add("slot-occupied");
                        slotButton.setOnAction(e -> showSlotInfo(slotId));
                    } else {
                        slotButton.getStyleClass().add("slot-vacant");
                        slotButton.setOnAction(null);
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Button findButton(Node root, String id) {
        if (root == null) return null;

        if (root instanceof Button && id.equals(root.getId())) {
            return (Button) root;
        }

        if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                Button found = findButton(child, id);
                if (found != null) return found;
            }

            // Special handling for ScrollPane (search inside its content)
            if (root instanceof javafx.scene.control.ScrollPane) {
                Node content = ((javafx.scene.control.ScrollPane) root).getContent();
                return findButton(content, id);
            }
        }

        return null;
    }
    private String getVehicleTypeFromFXML(String fxml) {
        switch (fxml) {
            case "motorcycles.fxml": return "Motorcycle";
            case "trucks.fxml": return "Truck";
            case "suvs.fxml": return "SUV";
            case "cars.fxml": return "Car";
            default: return "";
        }
    }


    private void showSlotInfo(String slotId) {
        String sql = """
        SELECT owner_name, plate_number, vehicle_type, brand, model, color, contact, time_in, duration, status
        FROM vehicles
        WHERE slot_id = ? AND (status = 'ACTIVE' OR status = 'OVERDUE')
        ORDER BY time_in DESC
        LIMIT 1
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, slotId);
            ResultSet rs = stmt.executeQuery();

            VBox contentBox = new VBox();
            contentBox.setSpacing(5);
            contentBox.setPadding(new Insets(10));

            String status;

            if (rs.next()) {
                buildVehicleDetails(contentBox, rs);
                status = rs.getString("status");
            } else {
                contentBox.getChildren().add(new Label("No active vehicle found in this slot."));
                status = "NONE";
            }

            showDetailsModal(slotId, contentBox, status);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading slot info", e.getMessage());
        }
    }

    private void addLabeledField(VBox box, String title, String value) {
        Label titleLabel = new Label(title + ": ");
        titleLabel.getStyleClass().add("label-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("label-value");

        HBox line = new HBox(titleLabel, valueLabel);
        line.setSpacing(4);
        box.getChildren().add(line);
    }

    private void buildVehicleDetails(VBox box, ResultSet rs) throws SQLException {
        String owner = rs.getString("owner_name");
        String plate = rs.getString("plate_number");
        String type = rs.getString("vehicle_type");
        String brand = rs.getString("brand");
        String model = rs.getString("model");
        String color = rs.getString("color");
        String contact = rs.getString("contact");
        Timestamp timeIn = rs.getTimestamp("time_in");
        int duration = rs.getInt("duration");
        String status = rs.getString("status");

        String formattedTimeIn = timeIn.toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        addLabeledField(box, "Owner", owner);
        addLabeledField(box, "Plate Number", plate);
        addLabeledField(box, "Vehicle Type", type);
        addLabeledField(box, "Brand/Model", brand + " " + model);
        addLabeledField(box, "Color", color);
        addLabeledField(box, "Contact", contact);
        addLabeledField(box, "Time In", formattedTimeIn);
        addLabeledField(box, "Duration", duration + " hours");

        Label statusLabel = new Label("STATUS: " + status.toUpperCase());
        statusLabel.getStyleClass().add(status.equalsIgnoreCase("OVERDUE") ? "status-overdue" : "status-active");
        box.getChildren().add(new Separator());
        box.getChildren().add(statusLabel);
    }

    private void showDetailsModal(String slotId, VBox content, String status) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Slot Information");
        alert.setHeaderText("Details for Slot: " + slotId);

        Platform.runLater(() -> {
            Label headerLabel = (Label) alert.getDialogPane().lookup(".header-panel .label");
            if (headerLabel != null) {
                headerLabel.getStyleClass().add("alert-header-text");
            }
        });

        Window window = contentArea.getScene().getWindow();
        alert.initOwner(window);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(250);

        alert.getDialogPane().setContent(scrollPane);

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/lotify/lotify/slot-info.css").toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("alert");

        if ("OVERDUE".equalsIgnoreCase(status)) {
            alert.getDialogPane().getStyleClass().add("alert-overdue");
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            alert.getDialogPane().getStyleClass().add("alert-active");
        }

        alert.showAndWait();
        Platform.runLater(() -> {
            alert.getDialogPane().lookupButton(ButtonType.OK)
                    .getStyleClass().add("button-ok");
        });

    }



    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



}

