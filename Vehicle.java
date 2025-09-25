package com.lotify.lotify;

import java.sql.Timestamp;

public class Vehicle {
    private String plateNumber;
    private String ownerName;
    private String contactNo;
    private String brand;
    private String model;
    private String color;
    private String type;
    private String slot;
    private int duration;
    private double payment;
    private Timestamp timeIn;
    private Timestamp timeOut;
    private String status;

    public Vehicle(String plateNumber, String ownerName, String contactNo,
                   String brand, String model, String color,
                   String type, String slot, int duration, double payment) {
        this.plateNumber = plateNumber;
        this.ownerName = ownerName;
        this.contactNo = contactNo;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.type = type;
        this.slot = slot;
        this.duration = duration;
        this.payment = payment;
        this.timeIn = new Timestamp(System.currentTimeMillis());
        this.timeOut = new Timestamp(timeIn.getTime() + (long) duration * 3600 * 1000);
    }

    // Getters & setters (only for existing fields)
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public double getPayment() { return payment; }
    public void setPayment(double payment) { this.payment = payment; }

    public Timestamp getTimeIn() { return timeIn; }
    public void setTimeIn(Timestamp timeIn) { this.timeIn = timeIn; }

    public Timestamp getTimeOut() { return timeOut; }
    public void setTimeOut(Timestamp timeOut) { this.timeOut = timeOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}