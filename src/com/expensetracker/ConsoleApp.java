package com.expensetracker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleApp {
    private ExpenseManager expenseManager;
    private BudgetManager budgetManager;

    // Absolute path to your data folder (on Desktop)
    private static final String DATA_DIR = System.getProperty("user.home") + "/Desktop/PERSONAL PROJECTS/Expense Tracker/";
    private static final String CSV_FILE = DATA_DIR + "expenses.csv";

    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> monthFilterBox;

    private JTextField dateField;
    private JComboBox<Object> categoryBox;
    private JTextField descField;
    private JTextField amountField;
    private JLabel salaryDisplayLabel;
    private JLabel remainingLabel;
    private JTextArea balanceHistoryArea;
    private static final String CONFIG_FILE = DATA_DIR + "config.properties";
    private double currentSalary = 0.0;

    public ConsoleApp() {
        Category.loadCategories();
        expenseManager = new ExpenseManager();
        budgetManager = new BudgetManager();

        // Create the directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try {
            expenseManager.loadFromCsv(CSV_FILE);
        } catch (Exception e) {
            // Start fresh if no file
        }

        initialize();
        loadSalary();
        updateRemainingBalance();
    }

    private void loadSalary() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                java.util.Properties props = new java.util.Properties();
                props.load(fis);
                String val = props.getProperty("salary", "0.00");
                currentSalary = Double.parseDouble(val);
                salaryDisplayLabel.setText("Monthly Salary: " + String.format("%.2f", currentSalary));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSalary(String salaryStr) {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_FILE)) {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("salary", salaryStr);
            props.store(fos, null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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
        table.setAutoCreateRowSorter(true); // Allow sorting
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Filter Panel ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("View Month:"));
        monthFilterBox = new JComboBox<>();
        updateMonthFilterOptions();
        monthFilterBox.setSelectedItem(YearMonth.now().toString());
        monthFilterBox.addActionListener(e -> {
            refreshTable();
            updateRemainingBalance();
        });
        filterPanel.add(monthFilterBox);

        JPanel salaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        salaryDisplayLabel = new JLabel("Monthly Salary: 0.00");
        salaryDisplayLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton setSalaryBtn = new JButton("Set Salary");
        setSalaryBtn.addActionListener(e -> setSalaryAction());
        salaryPanel.add(salaryDisplayLabel);
        salaryPanel.add(setSalaryBtn);

        refreshTable();

        JPanel northPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.WEST);
        topPanel.add(salaryPanel, BorderLayout.EAST);
        northPanel.add(topPanel, BorderLayout.NORTH);

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Expense"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        inputPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 3;
        inputPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 4;
        inputPanel.add(new JLabel(""), gbc);

        dateField = new JTextField(LocalDate.now().toString());
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(dateField, gbc);

        categoryBox = new JComboBox<>();
        refreshCategoryBox();
        categoryBox.addActionListener(e -> {
            if ("<EDIT CATEGORIES...>".equals(categoryBox.getSelectedItem())) {
                manageCategories();
            }
        });
        gbc.gridx = 1;
        inputPanel.add(categoryBox, gbc);

        descField = new JTextField();
        gbc.gridx = 2;
        inputPanel.add(descField, gbc);

        amountField = new JTextField();
        gbc.gridx = 3;
        inputPanel.add(amountField, gbc);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addExpense());
        gbc.gridx = 4;
        inputPanel.add(addButton, gbc);

        northPanel.add(inputPanel, BorderLayout.CENTER);
        frame.add(northPanel, BorderLayout.NORTH);

        // --- Action Buttons ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
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

        // --- Bottom Panel (Summary & Calculator) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        JPanel balanceHistoryPanel = new JPanel(new BorderLayout());
        balanceHistoryPanel.setBorder(BorderFactory.createTitledBorder("Balance After Each Expense"));
        balanceHistoryArea = new JTextArea(5, 30);
        balanceHistoryArea.setEditable(false);
        balanceHistoryPanel.add(new JScrollPane(balanceHistoryArea), BorderLayout.CENTER);

        JPanel calcAndBalancePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton calculatorButton = new JButton("Calculator");
        calculatorButton.addActionListener(e -> showCalculator());
        
        remainingLabel = new JLabel("Remaining: 0.00");
        remainingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        remainingLabel.setForeground(new Color(0, 128, 0));
        
        calcAndBalancePanel.add(calculatorButton);
        calcAndBalancePanel.add(remainingLabel);

        bottomPanel.add(balanceHistoryPanel, BorderLayout.CENTER);
        bottomPanel.add(calcAndBalancePanel, BorderLayout.EAST);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String selected = (String) monthFilterBox.getSelectedItem();
        List<Expense> toDisplay;
        if (selected != null && !selected.equals("All")) {
            YearMonth ym = YearMonth.parse(selected);
            toDisplay = expenseManager.getExpensesByMonth(ym.getMonthValue(), ym.getYear());
        } else {
            toDisplay = expenseManager.getAllExpenses();
        }
        
        for (Expense e : toDisplay) {
            tableModel.addRow(new Object[]{
                    e.getId(),
                    e.getDate(),
                    e.getCategory(),
                    e.getDescription(),
                    String.format("%.2f", e.getAmount())
            });
        }
    }

    private void updateMonthFilterOptions() {
        String currentSelection = (String) monthFilterBox.getSelectedItem();
        monthFilterBox.removeAllItems();
        monthFilterBox.addItem("All");
        
        java.util.Set<String> months = new java.util.TreeSet<>(Collections.reverseOrder());
        for (Expense e : expenseManager.getAllExpenses()) {
            months.add(YearMonth.from(e.getDate()).toString());
        }
        months.add(YearMonth.now().toString());
        
        for (String m : months) {
            monthFilterBox.addItem(m);
        }
        
        if (currentSelection != null) {
            monthFilterBox.setSelectedItem(currentSelection);
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
            updateMonthFilterOptions();
            refreshTable();
            updateRemainingBalance();
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

        // Convert view index to model index in case of sorting
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this expense?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseManager.deleteExpense(id);
            updateMonthFilterOptions();
            refreshTable();
            updateRemainingBalance();
            saveData();
        }
    }

    private void refreshCategoryBox() {
        categoryBox.removeAllItems();
        for (String catName : Category.getCategoryList()) {
            categoryBox.addItem(new Category(catName));
        }
        categoryBox.addItem("<EDIT CATEGORIES...>");
    }

    private void updateRemainingBalance() {
        String selected = (String) monthFilterBox.getSelectedItem();
        YearMonth targetYM;
        if (selected != null && !selected.equals("All")) {
            targetYM = YearMonth.parse(selected);
        } else {
            targetYM = YearMonth.now();
        }
        
        List<Expense> monthlyExpenses = expenseManager.getExpensesByMonth(targetYM.getMonthValue(), targetYM.getYear());
        double totalSpent = monthlyExpenses.stream().mapToDouble(Expense::getAmount).sum();
        
        double currentBalance = currentSalary;
        StringBuilder history = new StringBuilder();
        for (Expense e : monthlyExpenses) {
            currentBalance -= e.getAmount();
            history.append(String.format("%s: -%.2f | Rem: %.2f\n", e.getDescription(), e.getAmount(), currentBalance));
        }
        balanceHistoryArea.setText(history.toString());

        double remaining = currentSalary - totalSpent;
        remainingLabel.setText(String.format("Balance for %s: %.2f", targetYM, remaining));
        if (remaining < 0) {
            remainingLabel.setForeground(Color.RED);
        } else {
            remainingLabel.setForeground(new Color(0, 128, 0));
        }
    }

    private void setSalaryAction() {
        String input = JOptionPane.showInputDialog(frame, "Enter Monthly Salary:", String.format("%.2f", currentSalary));
        if (input != null) {
            try {
                double newSalary = Double.parseDouble(input.trim());
                currentSalary = newSalary;
                salaryDisplayLabel.setText("Monthly Salary: " + String.format("%.2f", currentSalary));
                saveSalary(String.format("%.2f", currentSalary));
                updateRemainingBalance();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid amount.");
            }
        }
    }

    private void manageCategories() {
        String[] options = {"Add Category", "Delete Category", "Cancel"};
        int choice = JOptionPane.showOptionDialog(frame, "What would you like to do?", "Manage Categories",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            String newCat = JOptionPane.showInputDialog(frame, "Enter new category name:");
            if (newCat != null && !newCat.trim().isEmpty()) {
                Category.addCategory(newCat.trim());
                refreshCategoryBox();
            }
        } else if (choice == 1) {
            List<String> cats = Category.getCategoryList();
            String toDelete = (String) JOptionPane.showInputDialog(frame, "Select category to delete:", "Delete Category",
                    JOptionPane.QUESTION_MESSAGE, null, cats.toArray(), cats.get(0));
            if (toDelete != null) {
                Category.deleteCategory(toDelete);
                refreshCategoryBox();
            }
        }
    }

    private void showCalculator() {
        JDialog calcDialog = new JDialog(frame, "Calculator", true);
        calcDialog.setSize(300, 400);
        calcDialog.setLayout(new BorderLayout());

        JTextField display = new JTextField("0");
        display.setFont(new Font("Arial", Font.BOLD, 24));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        calcDialog.add(display, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        String[] labels = {
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", "C", "=", "+"
        };

        final double[] result = {0};
        final String[] operator = {""};
        final boolean[] start = {true};

        for (String label : labels) {
            JButton btn = new JButton(label);
            btn.addActionListener(e -> {
                String cmd = e.getActionCommand();
                if ("0123456789".contains(cmd)) {
                    if (start[0]) {
                        display.setText(cmd);
                        start[0] = false;
                    } else {
                        display.setText(display.getText() + cmd);
                    }
                } else if (cmd.equals("C")) {
                    display.setText("0");
                    result[0] = 0;
                    operator[0] = "";
                    start[0] = true;
                } else if (cmd.equals("=")) {
                    calculate(Double.parseDouble(display.getText()), result, operator);
                    display.setText(String.valueOf(result[0]));
                    operator[0] = "";
                    start[0] = true;
                } else {
                    if (!operator[0].isEmpty()) {
                        calculate(Double.parseDouble(display.getText()), result, operator);
                        display.setText(String.valueOf(result[0]));
                    } else {
                        result[0] = Double.parseDouble(display.getText());
                    }
                    operator[0] = cmd;
                    start[0] = true;
                }
            });
            buttonsPanel.add(btn);
        }

        calcDialog.add(buttonsPanel, BorderLayout.CENTER);
        calcDialog.setLocationRelativeTo(frame);
        calcDialog.setVisible(true);
    }

    private void calculate(double n, double[] result, String[] operator) {
        if (operator[0].equals("+")) result[0] += n;
        else if (operator[0].equals("-")) result[0] -= n;
        else if (operator[0].equals("*")) result[0] *= n;
        else if (operator[0].equals("/")) {
            if (n != 0) result[0] /= n;
        }
        else if (operator[0].equals("")) result[0] = n;
    }

    private void setBudget() {
        List<String> catList = Category.getCategoryList();
        String catName = (String) JOptionPane.showInputDialog(frame, "Select Category:", "Set Budget",
                JOptionPane.QUESTION_MESSAGE, null, catList.toArray(), catList.get(0));
        if (catName == null) return;
        Category cat = new Category(catName);

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
            double balance = currentSalary - total;

            Map<String, Double> byCat = new HashMap<>();
            for (Expense e : monthlyExpenses) {
                String catName = e.getCategory().getName();
                byCat.put(catName, byCat.getOrDefault(catName, 0.0) + e.getAmount());
            }

            StringBuilder sb = new StringBuilder("Report for " + input + "\n");
            sb.append(String.format("Salary: %.2f\n", currentSalary));
            sb.append(String.format("Total Spent: %.2f\n", total));
            sb.append(String.format("Balance: %.2f\n\n", balance));
            sb.append("By Category:\n");
            
            List<String> sortedCats = new ArrayList<>(byCat.keySet());
            Collections.sort(sortedCats);

            for (String catName : sortedCats) {
                double amount = byCat.get(catName);
                sb.append(String.format("- %s: %.2f\n", catName, amount));
            }

            JOptionPane.showMessageDialog(frame, sb.toString());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid format. Use YYYY-MM.");
        }
    }

    private void saveData() {
        try {
            expenseManager.saveToCsv(CSV_FILE);
            saveSalary(String.format("%.2f", currentSalary));
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

            // Use the same directory as the CSV_FILE
            File dir = new File(DATA_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                JOptionPane.showMessageDialog(frame, "Cannot create directory: " + DATA_DIR);
                return;
            }

            String fileName = "Report_" + input + ".csv";
            File exportFile = new File(dir, fileName);

            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(exportFile))) {
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
                writer.write("Salary,,," + String.format("%.2f", currentSalary));
                writer.newLine();
                writer.write("Total Spent,,,=SUM(E2:E" + rowCount + ")");
                writer.newLine();
                writer.write("Balance,,,=D" + (rowCount + 2) + "-D" + (rowCount + 3));
                writer.newLine();

                JOptionPane.showMessageDialog(frame, "Report exported to " + exportFile.getAbsolutePath());
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