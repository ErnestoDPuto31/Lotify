package com.lotify.lotify;

public class Pricing {
    private int pricingId;
    private String type;
    private double ratePerHour;
    private double flatRate;
    private double dailyMax;

    public Pricing(int pricingId, String type, double ratePerHour, double flatRate, double dailyMax) {
        this.pricingId = pricingId;
        this.type = type;
        this.ratePerHour = ratePerHour;
        this.flatRate = flatRate;
        this.dailyMax = dailyMax;
    }

    public int getPricingId() { return pricingId; }
    public String getType() { return type; }
    public double getRatePerHour() { return ratePerHour; }
    public double getFlatRate() { return flatRate; }
    public double getDailyMax() { return dailyMax; }
}
