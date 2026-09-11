package com.expensetracker;

import java.io.*;
import java.util.*;

public class Category {
    private String name;
    private static List<String> categories = new ArrayList<>(Arrays.asList("FOOD", "TRANSPORT", "ENTERTAINMENT", "UTILITIES", "OTHER"));
    private static final String CATEGORIES_FILE = System.getProperty("user.home") + "/Desktop/PERSONAL PROJECTS/Expense Tracker/categories.txt";

    public Category(String name) {
        this.name = name.toUpperCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static List<String> getCategoryList() {
        return new ArrayList<>(categories);
    }

    public static void addCategory(String name) {
        String upperName = name.toUpperCase();
        if (!categories.contains(upperName)) {
            categories.add(upperName);
            saveCategories();
        }
    }

    public static void deleteCategory(String name) {
        String upperName = name.toUpperCase();
        if (categories.size() > 1) { // Keep at least one
            categories.remove(upperName);
            saveCategories();
        }
    }

    public static void loadCategories() {
        File file = new File(CATEGORIES_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                List<String> loaded = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        loaded.add(line.trim().toUpperCase());
                    }
                }
                if (!loaded.isEmpty()) {
                    categories = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveCategories() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CATEGORIES_FILE))) {
            for (String cat : categories) {
                writer.write(cat);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}