package com.DatabaseConnections;

import java.sql.*;

public class PricingDAO {

    public static double getRatePerHour(String vehicleType) {
        double rate = 0.0;
        String sql = "SELECT rate_per_hour FROM pricing WHERE vehicle_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleType);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                rate = rs.getDouble("rate_per_hour");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rate;
    }

    public static double getFlatRate(String vehicleType) {
        double flatRate = 0.0;
        String sql = "SELECT flat_rate FROM pricing WHERE vehicle_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleType);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                flatRate = rs.getDouble("flat_rate");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flatRate;
    }

    public static double getDailyMax(String vehicleType) {
        double dailyMax = 0.0;
        String sql = "SELECT daily_max FROM pricing WHERE vehicle_type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicleType);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                dailyMax = rs.getDouble("daily_max");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dailyMax;
    }
}

