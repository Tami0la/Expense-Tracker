package com.expensetracker;

import java.util.*;

public class BudgetManager {
    private Map<Category, Double> budgets;  // monthly budget per category

    public BudgetManager() {
        budgets = new HashMap<>();
    }

    public void setBudget(Category cat, double amount) {
        budgets.put(cat, amount);
    }

    public Double getBudget(Category cat) {
        return budgets.get(cat);   // null if not set
    }

    // Returns remaining budget; negative means overspent. Returns null if no budget set.
    public Double getRemaining(Category cat, int month, int year, ExpenseManager em) {
        Double budget = budgets.get(cat);
        if (budget == null) return null;

        double spent = em.getExpensesByMonth(month, year).stream()
                .filter(e -> e.getCategory() == cat)
                .mapToDouble(Expense::getAmount)
                .sum();
        return budget - spent;
    }

    // Returns a map of Category -> overspent amount (only for categories where budget is exceeded)
    public Map<Category, Double> checkAlerts(int month, int year, ExpenseManager em) {
        Map<Category, Double> alerts = new LinkedHashMap<>();
        for (Category cat : budgets.keySet()) {
            Double remaining = getRemaining(cat, month, year, em);
            if (remaining != null && remaining < 0) {
                alerts.put(cat, -remaining);  // store overspent amount
            }
        }
        return alerts;
    }
}