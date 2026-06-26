package com.expensetracker;

import java.time.LocalDate;

public class Expense {
    private int id;
    private LocalDate date;
    private Category category;
    private String description;
    private double amount;

    public Expense(int id, LocalDate date, Category category, String description, double amount) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
    }

    // Getters and setters (only those you really need to change after creation)
    public int getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | %.2f",
                id, date, category, description, amount);
    }

    // Convert this expense to a CSV line
    public String toCsvString() {
        return String.format("%d,%s,%s,\"%s\",%.2f",
                id, date, category,
                description.replace("\"", "\"\""),   // escape quotes
                amount);
    }

    // Create an Expense from a CSV line
    public static Expense fromCsvString(String csvLine) {
        // Simple CSV parser that respects quotes
        String[] parts = csvLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        int id = Integer.parseInt(parts[0].trim());
        LocalDate date = LocalDate.parse(parts[1].trim());
        Category category = Category.valueOf(parts[2].trim().toUpperCase());
        // Remove surrounding quotes and unescape double quotes
        String desc = parts[3].trim();
        if (desc.startsWith("\"") && desc.endsWith("\"")) {
            desc = desc.substring(1, desc.length() - 1).replace("\"\"", "\"");
        }
        double amount = Double.parseDouble(parts[4].trim());
        return new Expense(id, date, category, desc, amount);
    }
}