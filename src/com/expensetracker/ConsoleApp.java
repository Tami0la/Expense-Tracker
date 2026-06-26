package com.expensetracker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class ConsoleApp {
    private ExpenseManager expenseManager;
    private BudgetManager budgetManager;
    private static final String CSV_FILE = "expenses.csv";

    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField dateField;
    private JComboBox<Category> categoryBox;
    private JTextField descField;
    private JTextField amountField;
    private JTextField salaryField;

    public ConsoleApp() {
        expenseManager = new ExpenseManager();
        budgetManager = new BudgetManager();

        try {
            expenseManager.loadFromCsv(CSV_FILE);
        } catch (Exception e) {
            // Start fresh if no file
        }

        initialize();
    }

    private void initialize() {
        frame = new JFrame("Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // --- Table ---
        String[] columnNames = {"ID", "Date", "Category", "Description", "Amount"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        refreshTable();
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 6, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Expense"));

        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(new JLabel("Monthly Salary:"));
        inputPanel.add(new JLabel("")); // Empty for button placement

        dateField = new JTextField(LocalDate.now().toString());
        categoryBox = new JComboBox<>(Category.values());
        descField = new JTextField();
        amountField = new JTextField();
        salaryField = new JTextField("0.00");
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addExpense());

        inputPanel.add(dateField);
        inputPanel.add(categoryBox);
        inputPanel.add(descField);
        inputPanel.add(amountField);
        inputPanel.add(salaryField);
        inputPanel.add(addButton);

        frame.add(inputPanel, BorderLayout.NORTH);

        // --- Action Buttons ---
        JPanel actionPanel = new JPanel();
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteExpense());

        JButton setBudgetButton = new JButton("Set Budget");
        setBudgetButton.addActionListener(e -> setBudget());

        JButton viewAlertsButton = new JButton("View Alerts");
        viewAlertsButton.addActionListener(e -> viewAlerts());

        JButton reportButton = new JButton("Monthly Report");
        reportButton.addActionListener(e -> showReport());

        JButton exportButton = new JButton("Export to Excel");
        exportButton.addActionListener(e -> exportToExcel());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveData());

        actionPanel.add(deleteButton);
        actionPanel.add(setBudgetButton);
        actionPanel.add(viewAlertsButton);
        actionPanel.add(reportButton);
        actionPanel.add(exportButton);
        actionPanel.add(saveButton);

        frame.add(actionPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Expense e : expenseManager.getAllExpenses()) {
            tableModel.addRow(new Object[]{
                    e.getId(),
                    e.getDate(),
                    e.getCategory(),
                    e.getDescription(),
                    String.format("%.2f", e.getAmount())
            });
        }
    }

    private void addExpense() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            Category cat = (Category) categoryBox.getSelectedItem();
            String desc = descField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());

            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Description cannot be empty.");
                return;
            }

            expenseManager.addExpense(date, cat, desc, amount);
            refreshTable();
            descField.setText("");
            amountField.setText("");

            // Auto save
            saveData();
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid date format. Use YYYY-MM-DD.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount.");
        }
    }

    private void deleteExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense to delete.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this expense?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseManager.deleteExpense(id);
            refreshTable();
            saveData();
        }
    }

    private void setBudget() {
        Category cat = (Category) JOptionPane.showInputDialog(frame, "Select Category:", "Set Budget",
                JOptionPane.QUESTION_MESSAGE, null, Category.values(), Category.FOOD);
        if (cat == null) return;

        String amountStr = JOptionPane.showInputDialog(frame, "Enter monthly budget for " + cat + ":");
        if (amountStr == null) return;

        try {
            double amount = Double.parseDouble(amountStr);
            budgetManager.setBudget(cat, amount);
            JOptionPane.showMessageDialog(frame, "Budget set successfully.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount.");
        }
    }

    private void viewAlerts() {
        LocalDate now = LocalDate.now();
        Map<Category, Double> alerts = budgetManager.checkAlerts(now.getMonthValue(), now.getYear(), expenseManager);

        if (alerts.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No budget alerts for this month.");
        } else {
            StringBuilder sb = new StringBuilder("Budget Alerts for " + now.getMonth() + " " + now.getYear() + ":\n");
            for (var entry : alerts.entrySet()) {
                sb.append(String.format("- %s: Overspent by %.2f\n", entry.getKey(), entry.getValue()));
            }
            JOptionPane.showMessageDialog(frame, sb.toString());
        }
    }

    private void showReport() {
        String input = JOptionPane.showInputDialog(frame, "Enter Month and Year (YYYY-MM):", YearMonth.now().toString());
        if (input == null) return;

        try {
            YearMonth ym = YearMonth.parse(input);
            List<Expense> monthlyExpenses = expenseManager.getExpensesByMonth(ym.getMonthValue(), ym.getYear());

            if (monthlyExpenses.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No expenses found for " + input);
                return;
            }

            double total = monthlyExpenses.stream().mapToDouble(Expense::getAmount).sum();
            double salary = 0;
            try {
                salary = Double.parseDouble(salaryField.getText().trim());
            } catch (NumberFormatException nfe) {
                // Default to 0 if invalid
            }
            double balance = salary - total;

            Map<Category, Double> byCat = monthlyExpenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Expense::getCategory,
                            java.util.stream.Collectors.summingDouble(Expense::getAmount)));

            StringBuilder sb = new StringBuilder("Report for " + input + "\n");
            sb.append(String.format("Salary: %.2f\n", salary));
            sb.append(String.format("Total Spent: %.2f\n", total));
            sb.append(String.format("Balance: %.2f\n\n", balance));
            sb.append("By Category:\n");
            for (Category cat : Category.values()) {
                double amount = byCat.getOrDefault(cat, 0.0);
                sb.append(String.format("- %s: %.2f\n", cat, amount));
            }

            JOptionPane.showMessageDialog(frame, sb.toString());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid format. Use YYYY-MM.");
        }
    }

    private void saveData() {
        try {
            expenseManager.saveToCsv(CSV_FILE);
            // Since BudgetManager doesn't have save/load in ConsoleApp, I'm not adding it here to keep parity, 
            // though it would be a good addition. ConsoleApp also doesn't save budgets to file.
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error saving data: " + e.getMessage());
        }
    }

    private void exportToExcel() {
        String input = JOptionPane.showInputDialog(frame, "Enter Month and Year for Export (YYYY-MM):", YearMonth.now().toString());
        if (input == null) return;

        try {
            YearMonth ym = YearMonth.parse(input);
            List<Expense> monthlyExpenses = expenseManager.getExpensesByMonth(ym.getMonthValue(), ym.getYear());

            if (monthlyExpenses.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No expenses found for " + input);
                return;
            }

            double salary = 0;
            try {
                salary = Double.parseDouble(salaryField.getText().trim());
            } catch (NumberFormatException nfe) {
                // Default to 0 if invalid
            }

            String fileName = "Report_" + input + ".csv";
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(fileName))) {
                writer.write("ID,Date,Category,Description,Amount");
                writer.newLine();
                
                int rowCount = 1; // header is row 1
                for (Expense e : monthlyExpenses) {
                    writer.write(String.format("%d,%s,%s,\"%s\",%.2f",
                            e.getId(), e.getDate(), e.getCategory(), e.getDescription().replace("\"", "\"\""), e.getAmount()));
                    writer.newLine();
                    rowCount++;
                }
                
                writer.newLine();
                writer.write("Salary,,," + salary);
                writer.newLine();
                // Excel formula for total spent: SUM of column E from row 2 to rowCount
                writer.write("Total Spent,,,=SUM(E2:E" + rowCount + ")");
                writer.newLine();
                // Excel formula for balance: Salary - Total Spent
                // Salary is at E(rowCount+2), Total Spent is at E(rowCount+3)
                writer.write("Balance,,,=E" + (rowCount + 2) + "-E" + (rowCount + 3));
                writer.newLine();

                JOptionPane.showMessageDialog(frame, "Report exported to " + fileName);
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(frame, "Error exporting: " + e.getMessage());
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid format. Use YYYY-MM.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ConsoleApp::new);
    }
}
