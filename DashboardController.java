package com.Controllers;

import com.DatabaseConnections.TransactionDAO;
import com.DatabaseConnections.VehicleDAO;
import com.lotify.lotify.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

public class DashboardController {

    @FXML private LineChart<String, Number> lineChart;
    @FXML private PieChart pieChart;
    @FXML private TableView<Transaction> tableView;

    @FXML private Label occupiedLabel;
    @FXML private Label vacantLabel;
    @FXML private Label totalRevenueLabel;

    private static final int TOTAL_SLOTS = 260; // Set your actual total slot count

    @FXML
    public void initialize() {
        setupLineChart();
        setupPieChart();
        setupTable();
        setupMetrics(); // for revenue, occupied, vacant labels
    }

    // 📈 Weekly revenue (LineChart)
    private void setupLineChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Weekly Revenue");

        Map<Integer, Double> weeklyRevenue = new TreeMap<>();
        List<Transaction> allTransactions = TransactionDAO.getAllTransactions();

        for (Transaction tx : allTransactions) {
            if (tx.getTimeOut() != null) {
                LocalDateTime date = tx.getTimeOut().toLocalDateTime();
                int weekOfYear = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                weeklyRevenue.put(weekOfYear,
                        weeklyRevenue.getOrDefault(weekOfYear, 0.0) + tx.getPayment());
            }
        }

        for (Map.Entry<Integer, Double> entry : weeklyRevenue.entrySet()) {
            String label = "Week " + entry.getKey();
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        lineChart.getData().clear();
        lineChart.getData().add(series);
    }

    // Pie Chart (Occupied vs Available)
    private void setupPieChart() {
        int occupied = 0;

        VehicleDAO.updateAllOverdueVehicles(); // ensure accurate status

        var allVehicles = VehicleDAO.getAllVehicles();
        for (var v : allVehicles) {
            if (v.getStatus().equalsIgnoreCase("ACTIVE") || v.getStatus().equalsIgnoreCase("OVERDUE")) {
                occupied++;
            }
        }

        int available = TOTAL_SLOTS - occupied;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Occupied", occupied),
                new PieChart.Data("Available", available)
        );

        pieChart.setData(pieData);
    }

    // 10 latest transactions
    private void setupTable() {
        TableColumn<Transaction, String> colPlate = new TableColumn<>("Plate No");
        colPlate.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));

        TableColumn<Transaction, Timestamp> colEntry = new TableColumn<>("Entry Time");
        colEntry.setCellValueFactory(new PropertyValueFactory<>("timeIn"));

        TableColumn<Transaction, Timestamp> colExit = new TableColumn<>("Exit Time");
        colExit.setCellValueFactory(new PropertyValueFactory<>("timeOut"));

        TableColumn<Transaction, Double> colFee = new TableColumn<>("Fee Paid");
        colFee.setCellValueFactory(new PropertyValueFactory<>("payment"));

        tableView.getColumns().setAll(colPlate, colEntry, colExit, colFee);

        ObservableList<Transaction> recentTransactions = FXCollections.observableArrayList();
        List<Transaction> all = TransactionDAO.getAllTransactions();
        int limit = Math.min(all.size(), 10);
        recentTransactions.addAll(all.subList(0, limit));

        tableView.setItems(recentTransactions);
    }

    private void setupMetrics() {
        double totalRevenue = 0;
        int occupied = 0;

        List<Transaction> transactions = TransactionDAO.getAllTransactions();
        for (Transaction tx : transactions) {
            totalRevenue += tx.getPayment();
        }

        var allVehicles = VehicleDAO.getAllVehicles();
        for (var v : allVehicles) {
            if (v.getStatus().equalsIgnoreCase("ACTIVE") || v.getStatus().equalsIgnoreCase("OVERDUE")) {
                occupied++;
            }
        }

        int available = TOTAL_SLOTS - occupied;

        DecimalFormat df = new DecimalFormat("#,##0.00");

        totalRevenueLabel.setText("₱ " + df.format(totalRevenue));
        occupiedLabel.setText(String.valueOf(occupied));
        vacantLabel.setText(String.valueOf(available));
    }
}
