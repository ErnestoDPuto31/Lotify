package com.Controllers;

import com.DatabaseConnections.SlotsDAO;
import com.DatabaseConnections.VehicleDAO;
import com.lotify.lotify.NotificationManager;
import com.lotify.lotify.Vehicle;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Comparator;

public class VehiclesController {

    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> plateColumn;
    @FXML private TableColumn<Vehicle, String> ownerColumn;
    @FXML private TableColumn<Vehicle, String> contactColumn;
    @FXML private TableColumn<Vehicle, String> brandColumn;
    @FXML private TableColumn<Vehicle, String> modelColumn;
    @FXML private TableColumn<Vehicle, String> colorColumn;
    @FXML private TableColumn<Vehicle, String> typeColumn;
    @FXML private TableColumn<Vehicle, String> slotColumn;
    @FXML private TableColumn<Vehicle, Integer> durationColumn;
    @FXML private TableColumn<Vehicle, Double> paymentColumn;
    @FXML private TableColumn<Vehicle, String> timeInColumn;
    @FXML private TableColumn<Vehicle, String> timeOutColumn;
    @FXML private TableColumn<Vehicle, String> statusColumn;
    @FXML private ComboBox<String> vehicleFilterComboBox;
    @FXML private ComboBox<String> vehicleSortComboBox;
    @FXML private TextField vehicleSearchField;

    public static VehiclesController instance;

    private ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private FilteredList<Vehicle> filteredVehicles;
    private SortedList<Vehicle> sortedVehicles;

    private String lastSortField = "";
    private boolean ascending = true;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button removeButton;

    @FXML
    public void initialize() {
        instance = this;
        vehicles.setAll(VehicleDAO.getAllVehicles());
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        vehicleTable.setFixedCellSize(24);

        filteredVehicles = new FilteredList<>(vehicles, v -> true);
        sortedVehicles = new SortedList<>(filteredVehicles);
        vehicleTable.setItems(sortedVehicles);

        // Map table columns
        plateColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPlateNumber()));
        ownerColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOwnerName()));
        contactColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getContactNo()));
        brandColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getBrand()));
        modelColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getModel()));
        colorColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getColor()));
        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType()));
        slotColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSlot()));
        durationColumn.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuration()).asObject());
        paymentColumn.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getPayment()).asObject());
        statusColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("OVERDUE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if ("ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if ("EXITED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        timeInColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTimeIn() != null ? cd.getValue().getTimeIn().toString() : ""));
        timeOutColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTimeOut() != null ? cd.getValue().getTimeOut().toString() : ""));

        // Highlight columns
        addHighlighting(plateColumn, vehicleSearchField.textProperty());
        addHighlighting(ownerColumn, vehicleSearchField.textProperty());
        addHighlighting(contactColumn, vehicleSearchField.textProperty());
        addHighlighting(brandColumn, vehicleSearchField.textProperty());
        addHighlighting(modelColumn, vehicleSearchField.textProperty());
        addHighlighting(colorColumn, vehicleSearchField.textProperty());
        addHighlighting(typeColumn, vehicleSearchField.textProperty());
        addHighlighting(slotColumn, vehicleSearchField.textProperty());

        // Populate filter combo box
        vehicleFilterComboBox.setValue("Plate No.");
        vehicleFilterComboBox.getItems().addAll(
                "Plate No.", "Owner Name", "Contact No.",
                "Brand", "Model", "Color", "Type", "Slot"
        );

        // Populate sort combo box
        vehicleSortComboBox.getItems().addAll("Plate No.", "Owner Name", "Contact No.", "Brand", "Model", "Color", "Type", "Slot");
        vehicleSortComboBox.setOnAction(e -> applyCustomSort(vehicleSortComboBox.getValue()));

        // Live searching (unchanged)
        vehicleSearchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
        vehicleFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch(vehicleSearchField.getText()));

        javafx.animation.Timeline overdueChecker = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(10), e -> checkOverdueVehicles())
        );
        overdueChecker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        overdueChecker.play();

        checkOverdueVehicles();
    }

    private void handleSearch(String text) {
        String keyword = (text == null) ? "" : text.toLowerCase();
        String selectedFilter = vehicleFilterComboBox.getValue();

        filteredVehicles.setPredicate(vehicle -> {
            if (keyword.isEmpty()) return true;

            return switch (selectedFilter) {
                case "Plate No."   -> vehicle.getPlateNumber().toLowerCase().contains(keyword);
                case "Owner Name"  -> vehicle.getOwnerName().toLowerCase().contains(keyword);
                case "Contact No." -> vehicle.getContactNo().toLowerCase().contains(keyword);
                case "Brand"       -> vehicle.getBrand().toLowerCase().contains(keyword);
                case "Model"       -> vehicle.getModel().toLowerCase().contains(keyword);
                case "Color"       -> vehicle.getColor().toLowerCase().contains(keyword);
                case "Type"        -> vehicle.getType().toLowerCase().contains(keyword);
                case "Slot"        -> vehicle.getSlot().toLowerCase().contains(keyword);
                default            -> true;
            };
        });
    }

    private void applyCustomSort(String selectedSort) {
        if (selectedSort == null) return;

        Comparator<Vehicle> comparator = switch (selectedSort) {
            case "Plate No."   -> Comparator.comparing(Vehicle::getPlateNumber, String.CASE_INSENSITIVE_ORDER);
            case "Owner Name"  -> Comparator.comparing(Vehicle::getOwnerName, String.CASE_INSENSITIVE_ORDER);
            case "Contact No." -> Comparator.comparing(Vehicle::getContactNo, String.CASE_INSENSITIVE_ORDER);
            case "Brand"       -> Comparator.comparing(Vehicle::getBrand, String.CASE_INSENSITIVE_ORDER);
            case "Model"       -> Comparator.comparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER);
            case "Color"       -> Comparator.comparing(Vehicle::getColor, String.CASE_INSENSITIVE_ORDER);
            case "Type"        -> Comparator.comparing(Vehicle::getType, String.CASE_INSENSITIVE_ORDER);
            case "Slot"        -> Comparator.comparing(Vehicle::getSlot);
            default            -> null;
        };

        if (comparator != null) {
            if (selectedSort.equals(lastSortField)) {
                ascending = !ascending; // toggle if same field
            } else {
                ascending = true; // reset for new field
            }
            lastSortField = selectedSort;

            if (!ascending) comparator = comparator.reversed();

            sortedVehicles.setComparator(comparator);
        }
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lotify/lotify/add-car-dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(vehicleTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // draggable
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                dialogStage.setX(event.getScreenX() - xOffset[0]);
                dialogStage.setY(event.getScreenY() - yOffset[0]);
            });

            // Show dialog
            AddCarDialogController controller = loader.getController();
            dialogStage.showAndWait();

            Vehicle newVehicle = controller.getNewVehicle();
            if (newVehicle != null) {
                refreshTable(); // unified refresh
                NotificationManager.addInfo(newVehicle.getPlateNumber(), newVehicle.getSlot());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a vehicle to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lotify/lotify/add-car-dialog.fxml"));
            Parent root = loader.load();

            AddCarDialogController controller = loader.getController();
            controller.setEditingVehicle(selected);

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(vehicleTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // draggable window
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                dialogStage.setX(event.getScreenX() - xOffset[0]);
                dialogStage.setY(event.getScreenY() - yOffset[0]);
            });

            dialogStage.showAndWait();

            Vehicle newVehicle = controller.getNewVehicle();
            if (newVehicle != null) {
                refreshTable(); // unified refresh
                NotificationManager.addUpdate(newVehicle.getPlateNumber(), newVehicle.getSlot());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRemove() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            VehicleDAO.softDeleteVehicle(selected); // mark deleted
            SlotsDAO.freeSlot(selected.getSlot());

            refreshTable();
            NotificationManager.addExited(selected.getPlateNumber(), selected.getSlot());
        } else {
            showAlert("No Selection", "Please select a vehicle to remove.");
        }
    }

    @FXML
    private void handleRefreshButton() {
        VehicleDAO.updateAllOverdueVehicles();
        refreshTable();
    }

    public static void refreshTable() {
        if (instance != null) {
            instance.vehicles.setAll(VehicleDAO.getAllVehicles());
            instance.vehicleTable.refresh();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        Stage ownerStage = (Stage) vehicleTable.getScene().getWindow();

        if (ownerStage.getOwner() != null) {
            alert.initOwner(ownerStage.getOwner()); // attach to main app
        } else {
            alert.initOwner(ownerStage); // fallback
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void addHighlighting(TableColumn<Vehicle, String> column, StringProperty searchTextProperty) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);

                if (empty || item == null) return;

                String searchText = searchTextProperty.get() != null ? searchTextProperty.get().toLowerCase() : "";
                String itemText = item != null ? item : "";

                String selectedFilter = vehicleFilterComboBox.getValue();
                String thisColumnName = column.getText();

                if (!searchText.isEmpty()
                        && itemText.toLowerCase().contains(searchText)
                        && selectedFilter.equalsIgnoreCase(thisColumnName)) {

                    int start = itemText.toLowerCase().indexOf(searchText);
                    int end = start + searchText.length();

                    Text before = new Text(itemText.substring(0, start));
                    Text match = new Text(itemText.substring(start, end));
                    Text after = new Text(itemText.substring(end));

                    match.setFill(Color.RED);
                    match.setStyle("-fx-font-weight: bold;");

                    TextFlow flow = new TextFlow(before, match, after);
                    flow.setPrefHeight(Control.USE_COMPUTED_SIZE);
                    flow.setMaxHeight(Region.USE_PREF_SIZE);
                    flow.setMinHeight(Region.USE_PREF_SIZE);
                    flow.setLineSpacing(0);

                    setGraphic(flow);
                    setStyle("-fx-padding: 0 5 0 5;");
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                    setMaxHeight(Region.USE_PREF_SIZE);
                    setMinHeight(Region.USE_PREF_SIZE);
                } else {
                    setText(itemText);
                    setStyle("-fx-padding: 0 5 0 5;");
                    setGraphic(null);
                }
            }
        });
    }



    private void checkOverdueVehicles() {
        VehicleDAO.updateAllOverdueVehicles();
        refreshTable();

        for (Vehicle v : vehicles) {
            if ("OVERDUE".equalsIgnoreCase(v.getStatus())) {
                NotificationManager.addOverdue(v.getPlateNumber(), v.getSlot());
            }
        }
    }
}
