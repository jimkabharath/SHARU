package com.sharu.crm.ui;

public class LeadModel {
    private String name;
    private String phone;
    private String status;
    private String details;

    public LeadModel(String name, String phone, String status, String details) {
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.details = details;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
}
