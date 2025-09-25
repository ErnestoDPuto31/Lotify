package com.Controllers;

import com.DatabaseConnections.TransactionDAO;
import com.lotify.lotify.Transaction;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ChoiceBox<String> filterChoiceBox;

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> transactionIdColumn;
    @FXML private TableColumn<Transaction, String> ownerNameColumn;
    @FXML private TableColumn<Transaction, String> plateNumberColumn;
    @FXML private TableColumn<Transaction, Timestamp> timeInColumn;
    @FXML private TableColumn<Transaction, Timestamp> timeOutColumn;
    @FXML private TableColumn<Transaction, Integer> durationColumn;
    @FXML private TableColumn<Transaction, Timestamp> createdAtColumn;

    @FXML private Label totalTransactionsLabel;
    @FXML private Label totalAmountLabel;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> searchFilterChoiceBox;

    @FXML private Label detailSlotLabel;
    @FXML private Label detailOwnerLabel;
    @FXML private Label detailPlateLabel;
    @FXML private Label detailDurationLabel;
    @FXML private Label detailTotalLabel;

    @FXML private Button viewDetailsButton;
    @FXML private Button printReceiptButton;
    @FXML private Button adjustRefundButton;

    @FXML private Pagination pagination;
    private static final int ROWS_PER_PAGE = 25;

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<Transaction> currentPageData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Highlighting
        addHighlighting(ownerNameColumn, searchField.textProperty(), searchFilterChoiceBox.valueProperty(), "Owner Name");
        addHighlighting(plateNumberColumn, searchField.textProperty(), searchFilterChoiceBox.valueProperty(), "Plate Number");

        // Column resize behaviour

        ownerNameColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.15));
        plateNumberColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.12));
        transactionIdColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.10));
        timeInColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.15));
        timeOutColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.15));
        durationColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.13));
        createdAtColumn.prefWidthProperty().bind(transactionTable.widthProperty().multiply(0.20));

        // Cell factories
        transactionIdColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        ownerNameColumn.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        plateNumberColumn.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));
        timeInColumn.setCellValueFactory(new PropertyValueFactory<>("timeIn"));
        timeOutColumn.setCellValueFactory(new PropertyValueFactory<>("timeOut"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        transactionTable.setPlaceholder(new Label("No transactions found"));

        if (transactionTable.getParent() instanceof VBox) {
            VBox.setVgrow(transactionTable, Priority.ALWAYS);
        }

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) ->
                updateTablePage(newIndex == null ? 0 : newIndex.intValue())
        );

        fromDatePicker.valueProperty().addListener((obs, o, n) -> applyFilters());
        toDatePicker.valueProperty().addListener((obs, o, n) -> applyFilters());

        filterChoiceBox.setItems(FXCollections.observableArrayList("All", "Car", "Motorcycle", "SUV", "Truck"));
        filterChoiceBox.setValue("All");
        filterChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> applyFilters());

        searchFilterChoiceBox.setItems(FXCollections.observableArrayList("Plate Number", "Owner Name"));
        searchFilterChoiceBox.setValue("Plate Number");
        searchFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> applyFilters());

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            showTransactionDetails(newSelection);
        });


        loadTransactions();
    }

    private void loadTransactions() {
        List<Transaction> list = TransactionDAO.getAllTransactions();
        transactions.setAll(list);
        updatePagination();               // set up pages and show first page
        updateSummary(transactions);      // full summary of filtered set
    }

    private void applyFilters() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        String typeFilter = filterChoiceBox.getValue();
        String keyword = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String searchBy = searchFilterChoiceBox.getValue();

        List<Transaction> filtered = TransactionDAO.getAllTransactions().stream()
                .filter(t -> {
                    boolean match = true;

                    if (from != null) {
                        match = match && !t.getTimeIn().toLocalDateTime().toLocalDate().isBefore(from);
                    }
                    if (to != null && t.getTimeOut() != null) {
                        match = match && !t.getTimeOut().toLocalDateTime().toLocalDate().isAfter(to);
                    }

                    if (typeFilter != null && !"All".equals(typeFilter)) {
                        match = match && t.getVehicleType().equalsIgnoreCase(typeFilter);
                    }

                    if (!keyword.isEmpty()) {
                        if ("Plate Number".equals(searchBy)) {
                            match = match && t.getPlateNumber().toLowerCase().contains(keyword);
                        } else if ("Owner Name".equals(searchBy)) {
                            match = match && t.getOwnerName().toLowerCase().contains(keyword);
                        }
                    }

                    return match;
                })
                .collect(Collectors.toList());

        transactions.setAll(filtered);
        updatePagination();
        updateSummary(transactions);
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) transactions.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount == 0 ? 1 : pageCount);

        // If current index is out of range (after filtering), reset to 0
        if (pagination.getCurrentPageIndex() >= pagination.getPageCount()) {
            pagination.setCurrentPageIndex(0);
        } else {
            // ensure page updated to reflect current index (important after calling setPageCount)
            updateTablePage(pagination.getCurrentPageIndex());
        }
    }

    private void updateTablePage(int pageIndex) {
        if (transactions.isEmpty()) {
            currentPageData.clear();
            transactionTable.setItems(currentPageData);
            return;
        }

        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, transactions.size());

        currentPageData.setAll(transactions.subList(fromIndex, toIndex));
        transactionTable.setItems(currentPageData);
    }

    private void addHighlighting(TableColumn<Transaction, String> column,
                                 StringProperty searchTextProperty,
                                 ObservableValue<String> selectedFilterProperty,
                                 String filterName) {

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(null);
                setGraphic(null);

                if (empty || item == null) return;

                String searchText = searchTextProperty.get() != null ? searchTextProperty.get().toLowerCase() : "";
                String selectedFilter = selectedFilterProperty.getValue();

                if (!searchText.isEmpty()
                        && filterName.equalsIgnoreCase(selectedFilter)
                        && item.toLowerCase().contains(searchText)) {

                    int start = item.toLowerCase().indexOf(searchText);
                    int end = start + searchText.length();

                    javafx.scene.text.Text before = new javafx.scene.text.Text(item.substring(0, start));
                    javafx.scene.text.Text match = new javafx.scene.text.Text(item.substring(start, end));
                    javafx.scene.text.Text after = new javafx.scene.text.Text(item.substring(end));

                    match.setFill(javafx.scene.paint.Color.RED);
                    match.setStyle("-fx-font-weight: bold;");

                    javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(before, match, after);
                    flow.setLineSpacing(0);

                    flow.setPrefHeight(20);
                    flow.setMinHeight(20);
                    flow.setMaxHeight(20);

                    flow.setPrefWidth(Double.MAX_VALUE);

                    setGraphic(flow);
                    setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                } else {
                    setText(item);
                }
            }
        });
    }

    private void updateSummary(List<Transaction> list) {
        totalTransactionsLabel.setText(String.valueOf(list.size()));
        double totalAmount = list.stream().mapToDouble(Transaction::getPayment).sum();
        totalAmountLabel.setText(String.format("%.2f", totalAmount));
    }

    private void showTransactionDetails(Transaction t) {
        if (t != null) {
            detailSlotLabel.setText(t.getSlotId());
            detailOwnerLabel.setText(t.getOwnerName());
            detailPlateLabel.setText(t.getPlateNumber());
            detailDurationLabel.setText(t.getDuration() + " hrs");
            detailTotalLabel.setText(String.format("₱%.2f", t.getPayment()));
        } else {
            detailSlotLabel.setText("");
            detailOwnerLabel.setText("");
            detailPlateLabel.setText("");
            detailDurationLabel.setText("");
            detailTotalLabel.setText("");
        }
    }

    @FXML
    private void handleViewDetails() {
        Transaction t = transactionTable.getSelectionModel().getSelectedItem();
        if (t == null) {
            showAlert("No Selection", "Please select a transaction first.");
            return;
        }

        // Grid for details
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        // Labels
        grid.addRow(0, new Label("Transaction ID:"), new Label(String.valueOf(t.getTransactionId())));
        grid.addRow(1, new Label("Plate #:"), new Label(t.getPlateNumber()));
        grid.addRow(2, new Label("Owner:"), new Label(t.getOwnerName()));
        grid.addRow(3, new Label("Vehicle Type:"), new Label(t.getVehicleType()));
        grid.addRow(4, new Label("Slot:"), new Label(t.getSlotId()));
        grid.addRow(5, new Label("Time In:"), new Label(String.valueOf(t.getTimeIn())));
        grid.addRow(6, new Label("Time Out:"), new Label(String.valueOf(t.getTimeOut())));
        grid.addRow(7, new Label("Duration:"), new Label(t.getDuration() + " hrs"));
        grid.addRow(8, new Label("Payment:"), new Label("₱" + String.format("%.2f", t.getPayment())));

        // Create Alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Transaction Details");
        alert.setHeaderText("Transaction #" + t.getTransactionId());
        alert.getDialogPane().setContent(grid);

        // Optional: Apply CSS style to dialog
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/lotify/lotify/transactions.css").toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("custom-dialog");

        alert.initOwner(transactionTable.getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    private void handlePrintReceipt() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a transaction first.");
            return;
        }

        Transaction t = com.DatabaseConnections.TransactionDAO.getTransactionById(selected.getTransactionId());
        if (t == null) {
            showAlert("Error", "Failed to fetch updated transaction from database.");
            return;
        }

        try {
            java.nio.file.Path receiptsDir = java.nio.file.Paths.get("receipts");
            if (!java.nio.file.Files.exists(receiptsDir)) {
                java.nio.file.Files.createDirectories(receiptsDir);
            }

            String fileName = "receipts/receipt_" + t.getTransactionId() + ".pdf";

            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A6);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(fileName));
            document.open();

            // --- Title ---
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("LOTIFY: PARKING SYSTEM", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);

            document.add(new com.lowagie.text.Paragraph("\n"));

            // --- Transaction Details Table ---
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setWidths(new int[]{1, 2});

            com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12);

            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            addRow(table, "Transaction ID:", String.valueOf(t.getTransactionId()), boldFont, normalFont);
            addRow(table, "Plate Number:", t.getPlateNumber(), boldFont, normalFont);
            addRow(table, "Owner:", t.getOwnerName(), boldFont, normalFont);
            addRow(table, "Vehicle Type:", t.getVehicleType(), boldFont, normalFont);
            addRow(table, "Slot:", t.getSlotId(), boldFont, normalFont);
            addRow(table, "Time In:", t.getTimeIn() != null ? t.getTimeIn().toLocalDateTime().format(dtf) : "-", boldFont, normalFont);
            addRow(table, "Time Out:", t.getTimeOut() != null ? t.getTimeOut().toLocalDateTime().format(dtf) : "-", boldFont, normalFont);
            addRow(table, "Duration:", t.getDuration() + " hrs", boldFont, normalFont);
            addRow(table, "Payment:", "₱" + String.format("%.2f", t.getPayment()), boldFont, normalFont);

            document.add(table);

            // --- Thank You Note ---
            com.lowagie.text.Font thankFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLDITALIC);

            com.lowagie.text.Paragraph thanks = new com.lowagie.text.Paragraph("Thank you for parking with us!", thankFont);
            thanks.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(thanks);

            document.add(new com.lowagie.text.Paragraph("\n"));
            String generatedOn = "Generated On: " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            com.lowagie.text.Paragraph generated = new com.lowagie.text.Paragraph(
                    generatedOn,
                    new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.ITALIC)
            );
            generated.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            document.add(generated);

            document.close();

            java.awt.Desktop.getDesktop().open(new java.io.File(fileName));

            showAlert("Receipt Generated", "PDF receipt saved to:\n" + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to generate receipt: " + e.getMessage());
        }
    }

    // --- Helper method for table rows ---
    private void addRow(com.lowagie.text.pdf.PdfPTable table, String label, String value,
                        com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont) {
        com.lowagie.text.pdf.PdfPCell cell1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(label, labelFont));
        com.lowagie.text.pdf.PdfPCell cell2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(value, valueFont));

        cell1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);

        table.addCell(cell1);
        table.addCell(cell2);
    }

    @FXML
    private void handleAdjustRefund() {
        Transaction t = transactionTable.getSelectionModel().getSelectedItem();
        if (t == null) {
            showAlert("No Selection", "Please select a transaction first.");
            return;
        }

        // Prompt for new duration instead of payment
        TextInputDialog dialog = new TextInputDialog(String.valueOf(t.getDuration()));
        dialog.setTitle("Adjust/Refund");
        dialog.setHeaderText("Adjust duration for transaction #" + t.getTransactionId());
        dialog.setContentText("Enter new duration (hours):");

        dialog.initOwner(transactionTable.getScene().getWindow());

        dialog.showAndWait().ifPresent(newVal -> {
            try {
                int newDuration = Integer.parseInt(newVal);

                if (newDuration <= 0) {
                    showAlert("Invalid Input", "Duration must be greater than 0.");
                    return;
                }

                // Recalculate payment
                double flatRate = com.DatabaseConnections.PricingDAO.getFlatRate(t.getVehicleType());
                double ratePerHour = com.DatabaseConnections.PricingDAO.getRatePerHour(t.getVehicleType());

                double newPayment = (newDuration <= 2)
                        ? flatRate
                        : flatRate + (newDuration - 2) * ratePerHour;

                // Recalculate new Time Out
                java.time.LocalDateTime timeIn = t.getTimeIn().toLocalDateTime();
                java.time.LocalDateTime newTimeOut = timeIn.plusHours(newDuration);

                // Update DB: duration + payment + timeOut
                com.DatabaseConnections.TransactionDAO.updateDurationPaymentAndTimeOut(
                        t.getTransactionId(),
                        newDuration,
                        newPayment,
                        java.sql.Timestamp.valueOf(newTimeOut)
                );

                loadTransactions();
                showAlert("Success", "Duration, payment, and time-out updated successfully.");

            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number.");
            }
        });
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.initOwner(transactionTable.getScene().getWindow());
        alert.showAndWait();
    }
}

