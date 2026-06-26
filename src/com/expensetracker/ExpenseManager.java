package com.expensetracker;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseManager {
    private List<Expense> expenses;
    private static int nextId = 1;

    public ExpenseManager() {
        expenses = new ArrayList<>();
    }

    // ---------- CRUD ----------
    public Expense addExpense(LocalDate date, Category category, String description, double amount) {
        Expense e = new Expense(nextId++, date, category, description, amount);
        expenses.add(e);
        return e;
    }

    public boolean editExpense(int id, LocalDate newDate, Category newCat,
                               String newDesc, Double newAmount) {
        Expense e = findById(id);
        if (e == null) return false;
        if (newDate != null) e.setDate(newDate);
        if (newCat != null) e.setCategory(newCat);
        if (newDesc != null) e.setDescription(newDesc);
        if (newAmount != null) e.setAmount(newAmount);
        return true;
    }

    public boolean deleteExpense(int id) {
        return expenses.removeIf(e -> e.getId() == id);
    }

    public Expense findById(int id) {
        return expenses.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    public List<Expense> getAllExpenses() {
        return new ArrayList<>(expenses);   // defensive copy
    }

    // Filter by month and year (for reports)
    public List<Expense> getExpensesByMonth(int month, int year) {
        return expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == month && e.getDate().getYear() == year)
                .collect(Collectors.toList());
    }

    // ---------- CSV File I/O ----------
    public void saveToCsv(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("id,date,category,description,amount");
            writer.newLine();
            for (Expense e : expenses) {
                writer.write(e.toCsvString());
                writer.newLine();
            }
        }
    }

    public void loadFromCsv(String filename) throws IOException {
        expenses.clear();
        File file = new File(filename);
        if (!file.exists()) {
            // No save file yet – start fresh with nextId = 1
            nextId = 1;
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine(); // skip header
            String line;
            int maxId = 0;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Expense e = Expense.fromCsvString(line);
                expenses.add(e);
                if (e.getId() > maxId) maxId = e.getId();
            }
            nextId = maxId + 1;
        }
    }
}