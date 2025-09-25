package com.lotify.lotify;

import com.Controllers.VehiclesController;
import com.DatabaseConnections.DatabaseConnection;
import com.DatabaseConnections.VehicleDAO;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void scheduleExpiryNotification(String plateNumber, String slotId, LocalDateTime expiryTime) {
        long delay = Duration.between(LocalDateTime.now(), expiryTime).toMillis();

        if (delay <= 0) {
            handleOverdueNow(plateNumber, slotId);
            return;
        }

        scheduler.schedule(() -> handleOverdueNow(plateNumber, slotId), delay, TimeUnit.MILLISECONDS);
    }

    private static void handleOverdueNow(String plateNumber, String slotId) {

        if (!VehicleDAO.isDeleted(plateNumber)) {
            VehicleDAO.markAsOverdue(plateNumber);

            String message = NotificationManager.buildOverdueMessage(plateNumber, slotId);

            if (!NotificationManager.alreadyExists(message, NotificationType.OVERDUE)) {
                NotificationManager.addOverdue(plateNumber, slotId);
            }
            Platform.runLater(VehiclesController::refreshTable);
        }
    }

    // Reload expiry tasks from DB on startup
    public static void reloadFromDatabase() {
        String query = "SELECT plate_number, slot_id, time_in, duration, status, deleted " +
                "FROM vehicles WHERE time_out IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String plateNumber = rs.getString("plate_number");
                String slotId = rs.getString("slot_id");
                LocalDateTime timeIn = rs.getTimestamp("time_in").toLocalDateTime();
                int duration = rs.getInt("duration");
                LocalDateTime expiryTime = timeIn.plusHours(duration);
                boolean deleted = rs.getBoolean("deleted");
                String status = rs.getString("status");

                if (deleted) {
                    continue;
                }

                if (expiryTime.isBefore(LocalDateTime.now())
                        && "ACTIVE".equalsIgnoreCase(status)) {
                    handleOverdueNow(plateNumber, slotId);
                } else {
                    scheduleExpiryNotification(plateNumber, slotId, expiryTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
