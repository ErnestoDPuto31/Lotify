package com.lotify.lotify;

import com.DatabaseConnections.NotificationDAO;
import com.DatabaseConnections.VehicleDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NotificationManager {
    private static final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    static {
        notifications.addAll(NotificationDAO.loadNotifications());
    }

    public static ObservableList<Notification> getNotifications() {
        return notifications;
    }

    public static void add(String message, NotificationType type) {
        if (!notifications.isEmpty()) {
            Notification latest = notifications.get(0);
            if (latest.getMessage().equals(message) && latest.getType() == type) {
                return;
            }
        }

        Notification n = new Notification(message, type);
        notifications.add(0, n); // newest first
        NotificationDAO.saveNotification(n);
    }
    public static void addInfo(String plate, String slot) {
        add("Vehicle " + plate + " assigned to Slot " + slot, NotificationType.INFO);
    }
    public static void addUpdate(String plate, String slot) {
        add("Vehicle " + plate + " updated (Slot " + slot + ")", NotificationType.UPDATE);
    }
    public static void addExited(String plate, String slot) {
        add("Vehicle " + plate + " exited from Slot " + slot, NotificationType.EXITED);
    }
    public static void addOverdue(String plate, String slot) {
        if (isInTrash(plate)) return;
        String message = buildOverdueMessage(plate, slot);

        // Prevent duplicate overdue notifications for the same vehicle
        if (alreadyExists(message, NotificationType.OVERDUE)) {
            return;
        }

        add(message, NotificationType.OVERDUE);
    }
    public static String buildOverdueMessage(String plate, String slot) {
        return "Vehicle " + plate + " in slot " + slot + " has exceeded its allowed parking duration!";
    }
    private static boolean isInTrash(String plate) {
        return VehicleDAO.isDeleted(plate);
    }
    public static void addError(String message) {
        add("Error: " + message, NotificationType.ERROR);
    }
    public static boolean alreadyExists(String message, NotificationType type) {
        return notifications.stream()
                .anyMatch(n -> n.getMessage().equals(message) && n.getType() == type);
    }
    public static void clear() {
        notifications.clear();
        NotificationDAO.clearNotifications();
    }
}
