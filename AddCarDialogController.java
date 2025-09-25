package com.Controllers;

import com.DatabaseConnections.PricingDAO;
import com.DatabaseConnections.SlotsDAO;
import com.DatabaseConnections.VehicleDAO;
import com.DatabaseConnections.TransactionDAO;
import com.lotify.lotify.Vehicle;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.lotify.lotify.NotificationManager;

import java.util.List;

public class AddCarDialogController {

    @FXML private TextField plateField, ownerField, contactField, brandField, modelField, colorField;
    @FXML private ComboBox<String> typeComboBox, slotComboBox;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private Label paymentLabel;
    @FXML private Label timeInLabel;
    @FXML private Label timeOutLabel;

    private Vehicle newVehicle;

    @FXML
    public void initialize() {
        setupTypeComboBox();
        setupSlotComboBox();
        setupDurationSpinner();
        updateTimeLabels();

        contactField.setText("09");
        contactField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.startsWith("09")) {
                contactField.setText("09");
                contactField.positionCaret(2);
            }
        });
        contactField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                contactField.positionCaret(2);
            }
        });

        plateField.setOnAction(e -> ownerField.requestFocus());
        ownerField.setOnAction(e -> {
            contactField.requestFocus();
            contactField.positionCaret(2);
        });

        plateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^[A-Z0-9]{0,4}-?[A-Z0-9]{0,3}$")) {
                plateField.setStyle("-fx-border-color: #ff6c6c; -fx-border-width: 2px;");
            } else {
                plateField.setStyle("");
            }
        });

        contactField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^09\\d{0,9}$")) { // 09 + up to 9 digits
                contactField.setStyle("-fx-border-color: #ff6c6c; -fx-border-width: 2px;");
            } else {
                contactField.setStyle("");
            }
        });
        contactField.setOnAction(e -> brandField.requestFocus());
        brandField.setOnAction(e -> modelField.requestFocus());
        modelField.setOnAction(e -> colorField.requestFocus());
        colorField.setOnAction(e -> typeComboBox.requestFocus());

        typeComboBox.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                slotComboBox.requestFocus();
                e.consume();
            }
        });

        slotComboBox.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                durationSpinner.requestFocus();
                e.consume();
            }
        });

        durationSpinner.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSave();
                e.consume();
            }
        });

        typeComboBox.setOnAction(e -> {
            updatePayment();
            setupSlotComboBox();
        });
        durationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updatePayment();
            updateTimeLabels();
        });

        updatePayment();
    }




    private void setupTypeComboBox() {
        typeComboBox.getItems().addAll("Car", "Motorcycle", "SUV", "Truck");
    }
    private void setupSlotComboBox() {
        slotComboBox.getItems().clear();

        String selectedType = typeComboBox.getValue();
        if (selectedType == null) return;

        List<String> availableSlots = SlotsDAO.getAvailableSlots(selectedType);
        slotComboBox.getItems().addAll(availableSlots);
    }
    private void setupDurationSpinner() {
        durationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 1)
        );
    }
    private void updatePayment() {
        String type = typeComboBox.getValue();
        Integer duration = durationSpinner.getValue();

        if (type == null || duration == null) {
            paymentLabel.setText("0.00");
            return;
        }

        double flatRate = PricingDAO.getFlatRate(type);
        double ratePerHour = PricingDAO.getRatePerHour(type);

        double payment = (duration <= 2)
                ? flatRate
                : flatRate + (duration - 2) * ratePerHour;

        paymentLabel.setText(String.format("%.2f", payment));
    }
    private void updateTimeLabels() {
        Integer duration = durationSpinner.getValue();

        if (duration == null) {
            timeInLabel.setText("--:--");
            timeOutLabel.setText("--:--");
            return;
        }

        // Set Time-In as current time
        java.time.LocalDateTime timeIn = java.time.LocalDateTime.now();

        // Time-Out = Time-In + duration hours
        java.time.LocalDateTime timeOut = timeIn.plusHours(duration);

        // Format nicely
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        timeInLabel.setText(timeIn.format(formatter));
        timeOutLabel.setText(timeOut.format(formatter));
    }

    /** Save vehicle **/
    @FXML
    private void handleSave() {
        if (!validateInputs()) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.initOwner(plateField.getScene().getWindow());
        confirmAlert.setTitle("Confirm Save");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(editingVehicle == null
                ? "Are you sure you want to add this vehicle?"
                : "Are you sure you want to update this vehicle?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            Vehicle vehicleData = new Vehicle(
                    plateField.getText(),
                    ownerField.getText(),
                    contactField.getText(),
                    brandField.getText(),
                    modelField.getText(),
                    colorField.getText(),
                    typeComboBox.getValue(),
                    slotComboBox.getValue(),
                    durationSpinner.getValue(),
                    Double.parseDouble(paymentLabel.getText())
            );

            if (editingVehicle == null) {
                newVehicle = vehicleData;
                if (VehicleDAO.saveVehicle(newVehicle)) {
                    SlotsDAO.occupySlot(newVehicle.getSlot());

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle added successfully.");
                    NotificationManager.addInfo(newVehicle.getPlateNumber(), newVehicle.getSlot());
                    closeDialog();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Save Failed", "Failed to save vehicle. Please check your inputs.");
                }
            }
            else {
                vehicleData.setPlateNumber(editingVehicle.getPlateNumber());
                vehicleData.setTimeIn(editingVehicle.getTimeIn());
                vehicleData.setTimeOut(editingVehicle.getTimeOut());
                vehicleData.setStatus(editingVehicle.getStatus());
                vehicleData.setDuration(editingVehicle.getDuration());
                vehicleData.setPayment(editingVehicle.getPayment());

                if (VehicleDAO.updateVehicle(vehicleData)) {
                    if (!originalSlot.equals(vehicleData.getSlot())) {
                        SlotsDAO.freeSlot(originalSlot);
                        SlotsDAO.occupySlot(vehicleData.getSlot());
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle updated successfully.");
                    newVehicle = vehicleData;

                    TransactionDAO.updateTransactionFromVehicle(vehicleData.getPlateNumber());

                    NotificationManager.addUpdate(vehicleData.getPlateNumber(), vehicleData.getSlot());

                    closeDialog();
                }
                else {
                    showAlert(Alert.AlertType.ERROR, "Update Failed", "Failed to update vehicle.");
                }
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Payment must be a number.");
        }
    }


    /** Edit Vehicle Setter */
    private Vehicle editingVehicle = null;
    private String originalSlot = null;

    public void setEditingVehicle(Vehicle vehicle) {
        this.editingVehicle = vehicle;

        plateField.setText(vehicle.getPlateNumber());
        ownerField.setText(vehicle.getOwnerName());
        contactField.setText(vehicle.getContactNo());
        brandField.setText(vehicle.getBrand());
        modelField.setText(vehicle.getModel());
        colorField.setText(vehicle.getColor());
        typeComboBox.setValue(vehicle.getType());

        setupSlotComboBox();
        slotComboBox.setValue(vehicle.getSlot());

        durationSpinner.getValueFactory().setValue(vehicle.getDuration());
        durationSpinner.setDisable(true);

        paymentLabel.setText(String.format("%.2f", vehicle.getPayment()));
        originalSlot = vehicle.getSlot();
    }


    /** Cancel action */
    @FXML
    private void handleCancel() {
        newVehicle = null;
        closeDialog();
    }

    /** Validate required fields */
    private boolean validateInputs() {
        String plate = plateField.getText().trim();
        String owner = ownerField.getText().trim();
        String contact = contactField.getText().trim();
        String type = typeComboBox.getValue();
        String slot = slotComboBox.getValue();

        // Check empty fields
        if (plate.isEmpty() || owner.isEmpty() || contact.isEmpty()
                || type == null || slot == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields.");
            return false;
        }

        if (!plate.matches("^[A-Z0-9]{4}-[A-Z0-9]{3}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Plate Number",
                    "Plate number must follow format: XXXX-XXX (uppercase letters and digits only)");
            return false;
        }

        // Contact number format check
        if (!contact.matches("^09\\d{9}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Contact Number",
                    "Contact must be a valid PH number with 11 digits total.");
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);

        // This makes sure it attaches to the dialog window itself
        Stage dialogStage = (Stage) plateField.getScene().getWindow();
        alert.initOwner(dialogStage);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);

        alert.showAndWait();
    }

    private void closeDialog() {
        Stage stage = (Stage) plateField.getScene().getWindow();
        stage.close();
    }

    public Vehicle getNewVehicle() {
        return newVehicle;
    }
}
