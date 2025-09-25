package com.DatabaseConnections;

import javafx.scene.Node;
import javafx.scene.control.Button;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SlotsDAO {

    // Get available slots for a specific vehicle type
    public static List<String> getAvailableSlots(String vehicleType) {
        List<String> slots = new ArrayList<>();
        String query = "SELECT slot_id FROM slots WHERE vehicle_type = ? AND is_occupied = FALSE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, vehicleType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(rs.getString("slot_id"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return slots;
    }


    // Mark slot as occupied
    public static void occupySlot(String slotId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE slots SET is_occupied = TRUE WHERE slot_id = ?")) {
            ps.setString(1, slotId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Mark slot as available
    public static void freeSlot(String slotId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE slots SET is_occupied = FALSE WHERE slot_id = ?")) {
            ps.setString(1, slotId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
