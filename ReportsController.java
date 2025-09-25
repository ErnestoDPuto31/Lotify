package com.Controllers;

import com.DatabaseConnections.TransactionDAO;
import com.lotify.lotify.Transaction;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ReportsController {

    @FXML private ChoiceBox<String> chartTypeChoiceBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    @FXML private Button printButton;
    @FXML private Button saveButton;
    @FXML private Label avgRevenueLabel;
    @FXML private Label peakRevenueLabel;
    @FXML private Label lowestRevenueLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgParkTimeLabel;
    @FXML private Label totalParkTimeLabel;
    @FXML private Label totalCarsLabel;

    @FXML private AnchorPane chartContainer1;
    @FXML private AnchorPane chartContainer2;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM-dd");

    private XYChart<String, Number> revenueChart;
    private XYChart<String, Number> carsChart;
    private List<Transaction> lastFiltered = new ArrayList<>();

    @FXML
    public void initialize() {
        chartTypeChoiceBox.setItems(FXCollections.observableArrayList("Line Chart", "Bar Chart", "Area Chart"));
        chartTypeChoiceBox.setValue("Line Chart"); // default

        fromDatePicker.setValue(LocalDate.of(2025, 9, 9));
        toDatePicker.setValue(LocalDate.now());

        chartTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateReport());
        fromDatePicker.valueProperty().addListener((obs, o, n) -> updateReport());
        toDatePicker.valueProperty().addListener((obs, o, n) -> updateReport());

        updateReport(); // show initial charts
    }

    private void updateReport() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null || from.isAfter(to)) return;

        lastFiltered = TransactionDAO.getAllTransactions().stream()
                .filter(t -> {
                    LocalDate date = t.getTimeIn().toLocalDateTime().toLocalDate();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .collect(Collectors.toList());

        updateSummaryLabels(lastFiltered);
        updateCharts(lastFiltered, chartTypeChoiceBox.getValue());
    }

    private void updateSummaryLabels(List<Transaction> transactions) {
        double totalRevenue = transactions.stream().mapToDouble(Transaction::getPayment).sum();
        double avgRevenue = transactions.isEmpty() ? 0 : totalRevenue / transactions.size();

        int totalDuration = transactions.stream().mapToInt(Transaction::getDuration).sum();
        double avgDuration = transactions.isEmpty() ? 0 : (double) totalDuration / transactions.size();

        int totalCars = transactions.size();

        Map<LocalDate, Double> revenueByDay = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTimeIn().toLocalDateTime().toLocalDate(),
                        Collectors.summingDouble(Transaction::getPayment)
                ));

        double peak = revenueByDay.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double lowest = revenueByDay.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);

        avgRevenueLabel.setText(String.format("₱%.2f", avgRevenue));
        peakRevenueLabel.setText(String.format("₱%.2f", peak));
        lowestRevenueLabel.setText(String.format("₱%.2f", lowest));
        totalRevenueLabel.setText(String.format("₱%.2f", totalRevenue));

        avgParkTimeLabel.setText(String.format("%.2f hrs", avgDuration));
        totalParkTimeLabel.setText(totalDuration + " hrs");
        totalCarsLabel.setText(String.valueOf(totalCars));
    }

    private void updateCharts(List<Transaction> filtered, String chartType) {
        chartContainer1.getChildren().clear();
        chartContainer2.getChildren().clear();

        revenueChart = createChart(chartType, "Revenue Over Time", "Date", "Revenue (₱)");
        carsChart = createChart(chartType, "Cars Parked Over Time", "Date", "Cars");

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Daily Revenue");

        XYChart.Series<String, Number> carsSeries = new XYChart.Series<>();
        carsSeries.setName("Cars Parked");

        // Group transactions by day
        Map<LocalDate, List<Transaction>> groupedByDay = filtered.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTimeIn().toLocalDateTime().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<LocalDate, List<Transaction>> entry : groupedByDay.entrySet()) {
            String label = entry.getKey().format(dateFormat);

            double revenue = entry.getValue().stream().mapToDouble(Transaction::getPayment).sum();
            int cars = entry.getValue().size();

            revenueSeries.getData().add(new XYChart.Data<>(label, revenue));
            carsSeries.getData().add(new XYChart.Data<>(label, cars));
        }

        revenueChart.getData().add(revenueSeries);
        carsChart.getData().add(carsSeries);

        AnchorPane.setTopAnchor(revenueChart, 0.0);
        AnchorPane.setBottomAnchor(revenueChart, 0.0);
        AnchorPane.setLeftAnchor(revenueChart, 0.0);
        AnchorPane.setRightAnchor(revenueChart, 0.0);

        AnchorPane.setTopAnchor(carsChart, 0.0);
        AnchorPane.setBottomAnchor(carsChart, 0.0);
        AnchorPane.setLeftAnchor(carsChart, 0.0);
        AnchorPane.setRightAnchor(carsChart, 0.0);

        chartContainer1.getChildren().add(revenueChart);
        chartContainer2.getChildren().add(carsChart);
    }

    private XYChart<String, Number> createChart(String chartType, String title, String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        XYChart<String, Number> chart;
        switch (chartType) {
            case "Bar Chart" -> chart = new BarChart<>(xAxis, yAxis);
            case "Area Chart" -> chart = new AreaChart<>(xAxis, yAxis);
            default -> chart = new LineChart<>(xAxis, yAxis);
        }
        chart.setTitle(title);
        return chart;
    }

    @FXML
    private void onExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(chartContainer1.getScene().getWindow());

        if (file != null) {
            try {
                Document document = new Document(PageSize.A4, 40, 40, 40, 40);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Paragraph title = new Paragraph("Parking Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                document.add(new Paragraph("Generated on: " + LocalDate.now()));
                document.add(new Paragraph("Date Range: " + fromDatePicker.getValue() + " to " + toDatePicker.getValue()));
                document.add(new Paragraph("\n"));

                document.add(new Paragraph("Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                document.add(new Paragraph("Average Revenue: " + avgRevenueLabel.getText()));
                document.add(new Paragraph("Peak Revenue: " + peakRevenueLabel.getText()));
                document.add(new Paragraph("Lowest Revenue: " + lowestRevenueLabel.getText()));
                document.add(new Paragraph("Total Revenue: " + totalRevenueLabel.getText()));
                document.add(new Paragraph("Average Parking Time: " + avgParkTimeLabel.getText()));
                document.add(new Paragraph("Total Parking Time: " + totalParkTimeLabel.getText()));
                document.add(new Paragraph("Total Cars: " + totalCarsLabel.getText()));
                document.add(new Paragraph("\n\n"));

                addChartToPDF(document, revenueChart, "Revenue Over Time");
                addChartToPDF(document, carsChart, "Cars Parked Over Time");

                document.close();
                System.out.println("PDF saved: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addChartToPDF(Document document, XYChart<String, Number> chart, String title) throws Exception {
        if (chart == null) return;

        document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        document.add(new Paragraph("\n"));

        WritableImage fxImage = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

        File tempFile = File.createTempFile("chart", ".png");
        ImageIO.write(bufferedImage, "png", tempFile);

        Image chartImage = Image.getInstance(tempFile.getAbsolutePath());
        chartImage.scaleToFit(500, 300);
        chartImage.setAlignment(Element.ALIGN_CENTER);
        document.add(chartImage);
        document.add(new Paragraph("\n\n"));

        tempFile.deleteOnExit();
    }

    @FXML
    private void onExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(chartContainer1.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Plate Number,Vehicle Type,Owner Name,Time In,Time Out,Duration (hrs),Payment\n");

                for (Transaction t : lastFiltered) {
                    writer.append(t.getPlateNumber()).append(",")
                            .append(t.getVehicleType()).append(",")
                            .append(t.getOwnerName()).append(",")
                            .append(t.getTimeIn().toString()).append(",")
                            .append(t.getTimeOut() != null ? t.getTimeOut().toString() : "N/A").append(",")
                            .append(String.valueOf(t.getDuration())).append(",")
                            .append(String.valueOf(t.getPayment()))
                            .append("\n");
                }
                writer.flush();
                System.out.println("CSV saved: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
