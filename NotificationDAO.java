package com.DatabaseConnections;

import com.lotify.lotify.Notification;
import com.lotify.lotify.NotificationType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public static void saveNotification(Notification notification) {
        String sql = "INSERT INTO notifications (message, type, timestamp) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notification.getMessage());
            stmt.setString(2, notification.getType().name()); // ✅ save type as string
            stmt.setTimestamp(3, Timestamp.valueOf(notification.getTimestamp()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Notification> loadNotifications() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT message, type, timestamp FROM notifications ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String msg = rs.getString("message");
                String typeStr = rs.getString("type");
                LocalDateTime ts = rs.getTimestamp("timestamp").toLocalDateTime();

                NotificationType type;
                try {
                    type = NotificationType.valueOf(typeStr); // convert string to enum
                } catch (IllegalArgumentException | NullPointerException e) {
                    type = NotificationType.INFO; // fallback if invalid/missing
                }

                list.add(new Notification(msg, type, ts));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void clearNotifications() {
        String sql = "DELETE FROM notifications";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
