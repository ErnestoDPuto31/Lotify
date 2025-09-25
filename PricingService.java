package com.lotify.lotify;

import com.DatabaseConnections.PricingDAO;
import java.time.Duration;
import java.time.LocalDateTime;

public class PricingService {

    public static double calculatePayment(String vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        double flatRate = PricingDAO.getFlatRate(vehicleType);
        double dailyMax = PricingDAO.getDailyMax(vehicleType);

        // Get total hours (minimum 1 hour)
        long hours = Duration.between(entryTime, exitTime).toHours();
        if (hours < 1) hours = 1;

        double total;

        if (hours <= 2) {
            // Flat rate for first 2 hours
            total = flatRate;
        } else {
            // Flat rate + extra flat rate for every hour after 2 hours
            total = flatRate + (hours - 2) * flatRate;
        }

        // Apply daily max cap
        if (total > dailyMax) {
            total = dailyMax;
        }

        return total;
    }
}
