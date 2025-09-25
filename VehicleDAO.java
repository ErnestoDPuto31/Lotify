package com.DatabaseConnections;

import com.lotify.lotify.NotificationScheduler;
import com.lotify.lotify.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    // Save a new vehicle
    public static boolean saveVehicle(Vehicle vehicle) {
        String sql = "INSERT INTO vehicles " +
                "(plate_number, owner_name, contact, brand, model, color, vehicle_type, slot_id, duration, payment, time_in, time_out, status, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setString(2, vehicle.getOwnerName());
            stmt.setString(3, vehicle.getContactNo());
            stmt.setString(4, vehicle.getBrand());
            stmt.setString(5, vehicle.getModel());
            stmt.setString(6, vehicle.getColor());
            stmt.setString(7, vehicle.getType());
            stmt.setString(8, vehicle.getSlot());
            stmt.setInt(9, vehicle.getDuration());
            stmt.setDouble(10, vehicle.getPayment());
            stmt.setTimestamp(11, vehicle.getTimeIn());
            stmt.setTimestamp(12, vehicle.getTimeOut());
            stmt.setString(13, "ACTIVE");

            boolean success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int vehicleId = rs.getInt(1);
                        TransactionDAO.insertTransaction(vehicleId, vehicle);
                        LocalDateTime expiryTime = vehicle.getTimeIn().toLocalDateTime()
                                .plusHours(vehicle.getDuration());
                        NotificationScheduler.scheduleExpiryNotification(
                                vehicle.getPlateNumber(),
                                vehicle.getSlot(),
                                expiryTime
                        );
                    }
                }
            }

            return success;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles WHERE deleted = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Vehicle v = new Vehicle(
                        rs.getString("plate_number"),
                        rs.getString("owner_name"),
                        rs.getString("contact"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("color"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_id"),
                        rs.getInt("duration"),
                        rs.getDouble("payment")
                );
                v.setTimeIn(rs.getTimestamp("time_in"));
                v.setTimeOut(rs.getTimestamp("time_out"));
                v.setStatus(rs.getString("status"));
                vehicles.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return vehicles;
    }

    private static final List<String> VALID_COLUMNS = List.of(
            "plate_number", "owner_name", "contact", "brand", "model", "color", "vehicle_type", "slot_id"
    );

    // Update vehicle details
    public static boolean updateVehicle(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET " +
                "owner_name=?, contact=?, brand=?, model=?, color=?, " +
                "vehicle_type=?, slot_id=?, duration=?, payment=?, time_in=?, time_out=?, status=? " +
                "WHERE plate_number=? AND deleted = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vehicle.getOwnerName());
            ps.setString(2, vehicle.getContactNo());
            ps.setString(3, vehicle.getBrand());
            ps.setString(4, vehicle.getModel());
            ps.setString(5, vehicle.getColor());
            ps.setString(6, vehicle.getType());
            ps.setString(7, vehicle.getSlot());
            ps.setInt(8, vehicle.getDuration());
            ps.setDouble(9, vehicle.getPayment());
            ps.setTimestamp(10, vehicle.getTimeIn());
            ps.setTimestamp(11, vehicle.getTimeOut());

            // ✅ Use status from the controller, don't override
            ps.setString(12, vehicle.getStatus());

            ps.setString(13, vehicle.getPlateNumber());

            boolean success = ps.executeUpdate() > 0;

            if (success) {
                TransactionDAO.updateTransactionFromVehicle(vehicle.getPlateNumber());

                if (vehicle.getTimeOut() == null) {
                    LocalDateTime expiryTime = vehicle.getTimeIn().toLocalDateTime()
                            .plusHours(vehicle.getDuration());
                    NotificationScheduler.scheduleExpiryNotification(
                            vehicle.getPlateNumber(),
                            vehicle.getSlot(),
                            expiryTime
                    );
                }
            }

            return success;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Soft delete (move to Trash)
    public static boolean softDeleteVehicle(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET deleted = 1, status = 'EXITED' WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicle.getPlateNumber());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Vehicle> getDeletedVehicles() {
        List<Vehicle> deletedVehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles WHERE deleted = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Vehicle v = extractVehicle(rs);
                deletedVehicles.add(v);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deletedVehicles;
    }

    public static boolean restoreVehicle(String plateNumber) {
        String sql = "UPDATE vehicles SET deleted = 0 WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plateNumber);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hardDeleteVehicle(String plateNumber) {
        String sql = "DELETE FROM vehicles WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plateNumber);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void updateAllOverdueVehicles() {
        String sql = "UPDATE vehicles " +
                "SET status = 'OVERDUE' " +
                "WHERE time_out < NOW() " +   // expiry passed
                "AND status = 'ACTIVE' " +    // still active
                "AND deleted = 0";            // not deleted

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void markAsOverdue(String plateNumber) {
        String sql = "UPDATE vehicles SET status = 'OVERDUE' WHERE plate_number = ? AND deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plateNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // build Vehicle from ResultSet
    private static Vehicle extractVehicle(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle(
                rs.getString("plate_number"),
                rs.getString("owner_name"),
                rs.getString("contact"),
                rs.getString("brand"),
                rs.getString("model"),
                rs.getString("color"),
                rs.getString("vehicle_type"),
                rs.getString("slot_id"),
                rs.getInt("duration"),
                rs.getDouble("payment")
        );
        vehicle.setTimeIn(rs.getTimestamp("time_in"));
        vehicle.setTimeOut(rs.getTimestamp("time_out"));
        vehicle.setStatus(rs.getString("status"));
        return vehicle;
    }

    public static boolean isDeleted(String plateNumber) {
        String sql = "SELECT deleted FROM vehicles WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plateNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("deleted");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean emptyTrash() {
        String sql = "DELETE FROM vehicles WHERE deleted = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
