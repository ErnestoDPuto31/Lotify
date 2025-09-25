package com.lotify.lotify;

import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int vehicleId;
    private String plateNumber;
    private String vehicleType;
    private String ownerName;
    private String slotId;
    private Timestamp timeIn;
    private Timestamp timeOut;
    private int duration;
    private double payment;
    private Timestamp createdAt; // NEW

    public Transaction(int transactionId, int vehicleId, String plateNumber, String vehicleType,
                       String ownerName, String slotId, Timestamp timeIn,
                       Timestamp timeOut, int duration, double payment, Timestamp createdAt) {
        this.transactionId = transactionId;
        this.vehicleId = vehicleId;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.slotId = slotId;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.duration = duration;
        this.payment = payment;
        this.createdAt = createdAt;
    }

    public int getTransactionId() { return transactionId; }
    public String getPlateNumber() { return plateNumber; }
    public String getVehicleType() { return vehicleType; }
    public String getOwnerName() { return ownerName; }
    public String getSlotId() { return slotId; }
    public Timestamp getTimeIn() { return timeIn; }
    public Timestamp getTimeOut() { return timeOut; }
    public int getDuration() { return duration; }
    public double getPayment() { return payment; }
    public Timestamp getCreatedAt() { return createdAt; } // NEW
}
