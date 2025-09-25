package com.Controllers;

import com.DatabaseConnections.VehicleDAO;
import com.lotify.lotify.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TrashDialogController {

    @FXML private TableView<Vehicle> trashTable;
    @FXML private TableColumn<Vehicle, String> plateColumn;
    @FXML private TableColumn<Vehicle, String> ownerColumn;
    @FXML private TableColumn<Vehicle, String> typeColumn;
    @FXML private TableColumn<Vehicle, String> slotColumn;
    @FXML private TableColumn<Vehicle, String> timeInColumn;
    @FXML private TableColumn<Vehicle, String> timeOutColumn;

    @FXML private Button restoreButton;
    @FXML private Button deleteButton;
    @FXML private Button closeButton;
    @FXML private Button emptyButton;


    private final ObservableList<Vehicle> deletedVehicles = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Column bindings
        plateColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPlateNumber()));
        ownerColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getOwnerName()));
        typeColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
        slotColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getSlot()));
        timeInColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getTimeIn() != null ? cd.getValue().getTimeIn().toString() : ""));
        timeOutColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getTimeOut() != null ? cd.getValue().getTimeOut().toString() : ""));

        // Load soft-deleted vehicles from DB
        deletedVehicles.setAll(VehicleDAO.getDeletedVehicles());
        trashTable.setItems(deletedVehicles);

        // Button actions
        restoreButton.setOnAction(e -> handleRestore());
        deleteButton.setOnAction(e -> handlePermanentDelete());
        closeButton.setOnAction(e -> handleClose());
        emptyButton.setOnAction(e -> handleEmptyAll());
    }

    private void handleRestore() {
        Vehicle selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a vehicle to restore.");
            return;
        }

        boolean success = VehicleDAO.restoreVehicle(selected.getPlateNumber());
        if (success) {
            deletedVehicles.remove(selected);
            VehiclesController.refreshTable();
            showAlert("Success", "Vehicle restored successfully.");
        } else {
            showAlert("Error", "Failed to restore vehicle.");
        }
    }

    private void handlePermanentDelete() {
        Vehicle selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a vehicle to delete permanently.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will permanently delete the vehicle. Continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);

        Stage dialogStage = (Stage) closeButton.getScene().getWindow();
        if (dialogStage != null) {
            confirm.initOwner(dialogStage);
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean success = VehicleDAO.hardDeleteVehicle(selected.getPlateNumber());
                if (success) {
                    deletedVehicles.remove(selected);
                    VehiclesController.refreshTable();
                    showAlert("Deleted", "Vehicle permanently deleted.");
                } else {
                    showAlert("Error", "Failed to delete vehicle.");
                }
            }
        });
    }

    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void handleEmptyAll() {
        if (deletedVehicles.isEmpty()) {
            showAlert("Empty", "Trash is already empty.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will permanently delete ALL vehicles in trash. Continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Empty All");
        confirm.setHeaderText(null);

        Stage dialogStage = (Stage) closeButton.getScene().getWindow();
        if (dialogStage != null) {
            confirm.initOwner(dialogStage);
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean success = VehicleDAO.emptyTrash(); // 🔹 implement in DAO
                if (success) {
                    deletedVehicles.clear();
                    VehiclesController.refreshTable();
                    showAlert("Emptied", "All vehicles permanently deleted.");
                } else {
                    showAlert("Error", "Failed to empty trash.");
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Stage dialogStage = (Stage) closeButton.getScene().getWindow();
        if (dialogStage != null) {
            alert.initOwner(dialogStage);
        }

        alert.showAndWait();
    }
}
