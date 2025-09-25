package com.DatabaseConnections;

import com.lotify.lotify.Transaction;
import com.lotify.lotify.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public static boolean insertTransaction(int vehicleId, Vehicle vehicle) {
        String sql = "INSERT INTO transactions " +
                "(vehicle_id, plate_number, vehicle_type, owner_name, slot_id, time_in, time_out, duration, payment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, vehicleId);
            ps.setString(2, vehicle.getPlateNumber());
            ps.setString(3, vehicle.getType());
            ps.setString(4, vehicle.getOwnerName());
            ps.setString(5, vehicle.getSlot());
            ps.setTimestamp(6, vehicle.getTimeIn());
            ps.setTimestamp(7, vehicle.getTimeOut());
            ps.setInt(8, vehicle.getDuration());
            ps.setDouble(9, vehicle.getPayment());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("plate_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("owner_name"),
                        rs.getString("slot_id"),
                        rs.getTimestamp("time_in"),
                        rs.getTimestamp("time_out"),
                        rs.getInt("duration"),
                        rs.getDouble("payment"),
                        rs.getTimestamp("created_at")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean updateDurationPaymentAndTimeOut(int transactionId, int duration, double payment, Timestamp newTimeOut) {
        String sql = "UPDATE transactions SET duration = ?, payment = ?, time_out = ? WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, duration);
            stmt.setDouble(2, payment);
            stmt.setTimestamp(3, newTimeOut);
            stmt.setInt(4, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Transaction getTransactionById(int transactionId) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("plate_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("owner_name"),
                        rs.getString("slot_id"),
                        rs.getTimestamp("time_in"),
                        rs.getTimestamp("time_out"),
                        rs.getInt("duration"),
                        rs.getDouble("payment"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateTransactionFromVehicle(String plateNumber) {
        String findSql = "SELECT transaction_id FROM transactions WHERE plate_number = ? ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findSql)) {

            findStmt.setString(1, plateNumber);
            ResultSet rs = findStmt.executeQuery();

            if (rs.next()) {
                int transactionId = rs.getInt("transaction_id");

                String updateSql = "UPDATE transactions t " +
                        "JOIN vehicles v ON t.plate_number = v.plate_number " +
                        "SET t.vehicle_type = v.vehicle_type, " +
                        "    t.owner_name = v.owner_name, " +
                        "    t.slot_id = v.slot_id, " +  // ✅ use correct column name
                        "    t.duration = v.duration, " +
                        "    t.payment = v.payment, " +
                        "    t.time_in = v.time_in, " +
                        "    t.time_out = v.time_out " +
                        "WHERE t.transaction_id = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, transactionId);
                    return updateStmt.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
