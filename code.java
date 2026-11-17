package com.bikerental;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                DB.initialize();
                new LoginFrame().setVisible(true);
            } catch (Exception ex) {
                showError(null, "Could not initialize the application/database. Check MySQL and DB settings.", ex);
            }
        });
    }

    static void showError(Component parent, String message, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(parent, message + "\n\n" + ex.getMessage(),
                "Bike Rental System", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Bike Rental System", JOptionPane.INFORMATION_MESSAGE);
    }

    static JTextField field(int columns) { return new JTextField(columns); }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    static class LoginFrame extends JFrame {
        private final JTextField username = new JTextField("admin", 18);
        private final JPasswordField password = new JPasswordField(18);

        LoginFrame() {
            setTitle("Bike Rental System - Login");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(390, 250);
            setLocationRelativeTo(null);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(new EmptyBorder(20, 28, 20, 28));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(7, 7, 7, 7);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
            JLabel title = new JLabel("BIKE RENTAL MANAGEMENT", SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
            panel.add(title, c);
            c.gridwidth = 1; c.gridy++;
            panel.add(new JLabel("Username"), c);
            c.gridx = 1; panel.add(username, c);
            c.gridx = 0; c.gridy++; panel.add(new JLabel("Password"), c);
            c.gridx = 1; panel.add(password, c);
            c.gridx = 0; c.gridy++; c.gridwidth = 2;
            JButton login = new JButton("Login");
            panel.add(login, c);
            login.addActionListener(e -> login());
            password.addActionListener(e -> login());
            add(panel);
        }

        private void login() {
            String sql = "SELECT password_hash FROM admins WHERE username=?";
            try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username.getText().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && Security.verifyPassword(new String(password.getPassword()), rs.getString(1))) {
                        new DashboardFrame().setVisible(true);
                        dispose();
                    } else info(this, "Invalid username or password.");
                }
            } catch (Exception ex) { showError(this, "Login failed.", ex); }
        }
    }

    static class DashboardFrame extends JFrame {
        DashboardFrame() {
            setTitle("Bike Rental Management System");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1000, 650);
            setLocationRelativeTo(null);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Bikes", new BikesPanel());
            tabs.addTab("Customers", new CustomersPanel());
            tabs.addTab("Rentals & Returns", new RentalsPanel());
            JLabel footer = new JLabel("  Bike Rental Management  |  JDBC + MySQL", SwingConstants.LEFT);
            add(tabs, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }
    }

    static class BikesPanel extends JPanel {
        private final DefaultTableModel table = model("ID", "Bike name", "Type", "Price/day", "Status");
        private final JTable grid = new JTable(table);
        private final JTextField id = field(5), name = field(14), type = field(12), price = field(8);

        BikesPanel() {
            setLayout(new BorderLayout(8, 8)); setBorder(new EmptyBorder(10,10,10,10));
            grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            add(new JScrollPane(grid), BorderLayout.CENTER);
            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.add(new JLabel("ID")); form.add(id);
            form.add(new JLabel("Name")); form.add(name);
            form.add(new JLabel("Type")); form.add(type);
            form.add(new JLabel("Price/day")); form.add(price);
            JButton add = new JButton("Add");
            JButton update = new JButton("Update");
            JButton delete = new JButton("Delete");
            JButton clear = new JButton("Clear");
            JButton refresh = new JButton("Refresh");
            form.add(add); form.add(update); form.add(delete); form.add(clear); form.add(refresh);
            add(form, BorderLayout.SOUTH);
            grid.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && grid.getSelectedRow() >= 0) {
                    int r = grid.getSelectedRow();
                    id.setText(table.getValueAt(r,0).toString());
                    name.setText(table.getValueAt(r,1).toString());
                    type.setText(table.getValueAt(r,2).toString());
                    price.setText(table.getValueAt(r,3).toString());
                }
            });
            add.addActionListener(e -> saveBike(false));
            update.addActionListener(e -> saveBike(true));
            delete.addActionListener(e -> deleteBike());
            clear.addActionListener(e -> clearForm());
            refresh.addActionListener(e -> load());
            load();
        }

        private void saveBike(boolean update) {
            try {
                String n = name.getText().trim(), t = type.getText().trim();
                BigDecimal p = new BigDecimal(price.getText().trim());
                if (n.isEmpty() || t.isEmpty() || p.signum() < 0) throw new IllegalArgumentException("Enter a name, type and non-negative price.");
                String sql = update
                        ? "UPDATE bikes SET bike_name=?,bike_type=?,price_per_day=? WHERE bike_id=? AND status='AVAILABLE'"
                        : "INSERT INTO bikes(bike_name,bike_type,price_per_day) VALUES(?,?,?)";
                try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1,n); ps.setString(2,t); ps.setBigDecimal(3,p);
                    if (update) ps.setInt(4, Integer.parseInt(id.getText().trim()));
                    int count = ps.executeUpdate();
                    info(this, count > 0 ? (update ? "Bike updated." : "Bike added.") : "No bike changed. A rented bike cannot be edited.");
                }
                clearForm(); load();
            } catch (Exception ex) { showError(this, "Could not save bike. Check the values.", ex); }
        }

        private void deleteBike() {
            try {
                if (id.getText().isBlank()) { info(this, "Select a bike first."); return; }
                int answer = JOptionPane.showConfirmDialog(this, "Delete selected bike?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) return;
                try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM bikes WHERE bike_id=? AND status='AVAILABLE'")) {
                    ps.setInt(1, Integer.parseInt(id.getText().trim()));
                    int n = ps.executeUpdate();
                    info(this, n > 0 ? "Bike deleted." : "Bike is rented or has rental history and cannot be deleted.");
                }
                clearForm(); load();
            } catch (Exception ex) { showError(this, "Could not delete bike.", ex); }
        }

        private void clearForm() { id.setText(""); name.setText(""); type.setText(""); price.setText(""); grid.clearSelection(); }
        private void load() {
            table.setRowCount(0);
            try (Connection con = DB.getConnection(); Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT bike_id,bike_name,bike_type,price_per_day,status FROM bikes ORDER BY bike_id DESC")) {
                while (rs.next()) table.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getString(5)});
            } catch (Exception ex) { showError(this, "Could not load bikes.", ex); }
        }
    }

    static class CustomersPanel extends JPanel {
        private final DefaultTableModel table = model("ID", "Customer name", "Phone", "Email");
        private final JTable grid = new JTable(table);
        private final JTextField id = field(5), name = field(16), phone = field(12), email = field(18);

        CustomersPanel() {
            setLayout(new BorderLayout(8,8)); setBorder(new EmptyBorder(10,10,10,10));
            add(new JScrollPane(grid), BorderLayout.CENTER);
            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.add(new JLabel("ID")); form.add(id); form.add(new JLabel("Name")); form.add(name);
            form.add(new JLabel("Phone")); form.add(phone); form.add(new JLabel("Email")); form.add(email);
            JButton add = new JButton("Add"), update = new JButton("Update"), delete = new JButton("Delete"), clear = new JButton("Clear"), refresh = new JButton("Refresh");
            form.add(add); form.add(update); form.add(delete); form.add(clear); form.add(refresh);
            add(form, BorderLayout.SOUTH);
            grid.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && grid.getSelectedRow() >= 0) {
                    int r=grid.getSelectedRow(); id.setText(table.getValueAt(r,0).toString());
                    name.setText(table.getValueAt(r,1).toString()); phone.setText(table.getValueAt(r,2).toString());
                    email.setText(String.valueOf(table.getValueAt(r,3)));
                }
            });
            add.addActionListener(e -> save(false)); update.addActionListener(e -> save(true));
            delete.addActionListener(e -> delete()); clear.addActionListener(e -> clearForm()); refresh.addActionListener(e -> load());
            load();
        }

        private void save(boolean update) {
            try {
                String n=name.getText().trim(), p=phone.getText().trim(), em=email.getText().trim();
                if(n.isEmpty() || p.isEmpty()) throw new IllegalArgumentException("Customer name and phone are required.");
                String sql=update ? "UPDATE customers SET customer_name=?,phone=?,email=? WHERE customer_id=?"
                        : "INSERT INTO customers(customer_name,phone,email) VALUES(?,?,?)";
                try(Connection con=DB.getConnection(); PreparedStatement ps=con.prepareStatement(sql)) {
                    ps.setString(1,n); ps.setString(2,p); ps.setString(3,em.isEmpty()?null:em);
                    if(update) ps.setInt(4,Integer.parseInt(id.getText().trim()));
                    ps.executeUpdate();
                }
                info(this, update?"Customer updated.":"Customer added."); clearForm(); load();
            } catch(Exception ex) { showError(this,"Could not save customer.",ex); }
        }

        private void delete() {
            try {
                if(id.getText().isBlank()) { info(this,"Select a customer first."); return; }
                if(JOptionPane.showConfirmDialog(this,"Delete selected customer?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
                try(Connection con=DB.getConnection(); PreparedStatement ps=con.prepareStatement("DELETE FROM customers WHERE customer_id=?")) {
                    ps.setInt(1,Integer.parseInt(id.getText().trim())); ps.executeUpdate();
                }
                clearForm(); load();
            } catch(Exception ex) { showError(this,"Could not delete customer. Customers with rental history must be retained.",ex); }
        }
        private void clearForm(){id.setText("");name.setText("");phone.setText("");email.setText("");grid.clearSelection();}
        private void load(){
            table.setRowCount(0);
            try(Connection con=DB.getConnection(); Statement st=con.createStatement();
                ResultSet rs=st.executeQuery("SELECT customer_id,customer_name,phone,email FROM customers ORDER BY customer_id DESC")){
                while(rs.next()) table.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)});
            } catch(Exception ex){showError(this,"Could not load customers.",ex);}
        }
    }

    static class RentalsPanel extends JPanel {
        private final DefaultTableModel table = model("Rental ID","Customer","Bike","Rent date","Due date","Returned date","Amount","Status");
        private final JTable grid = new JTable(table);
        private final JComboBox<Choice> customer = new JComboBox<>();
        private final JComboBox<Choice> bike = new JComboBox<>();
        private final JTextField days = field(5);
        private final JLabel estimate = new JLabel("Estimated total: -");

        RentalsPanel() {
            setLayout(new BorderLayout(8,8)); setBorder(new EmptyBorder(10,10,10,10));
            add(new JScrollPane(grid),BorderLayout.CENTER);
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(new JLabel("Customer")); top.add(customer);
            top.add(new JLabel("Available bike")); top.add(bike);
            top.add(new JLabel("Days")); top.add(days);
            JButton estimateButton=new JButton("Estimate"), rentButton=new JButton("Rent bike");
            JButton returnButton=new JButton("Return selected"), refresh=new JButton("Refresh");
            top.add(estimateButton); top.add(rentButton); top.add(returnButton); top.add(refresh); top.add(estimate);
            add(top,BorderLayout.NORTH);
            estimateButton.addActionListener(e -> estimate());
            rentButton.addActionListener(e -> rent());
            returnButton.addActionListener(e -> returnBike());
            refresh.addActionListener(e -> reloadAll());
            reloadAll();
        }

        private void loadChoices() throws SQLException {
            customer.removeAllItems(); bike.removeAllItems();
            try(Connection con=DB.getConnection(); Statement st=con.createStatement();
                ResultSet rs=st.executeQuery("SELECT customer_id,customer_name FROM customers ORDER BY customer_name")){
                while(rs.next()) customer.addItem(new Choice(rs.getInt(1),rs.getString(2)));
            }
            try(Connection con=DB.getConnection(); Statement st=con.createStatement();
                ResultSet rs=st.executeQuery("SELECT bike_id,CONCAT(bike_name,' (',bike_type,') - ',price_per_day,'/day') FROM bikes WHERE status='AVAILABLE' ORDER BY bike_name")){
                while(rs.next()) bike.addItem(new Choice(rs.getInt(1),rs.getString(2)));
            }
        }

        private void estimate() {
            try {
                Choice selected=(Choice)bike.getSelectedItem();
                int number=Integer.parseInt(days.getText().trim());
                if(selected==null || number<1) throw new IllegalArgumentException("Choose an available bike and enter at least 1 day.");
                try(Connection con=DB.getConnection(); PreparedStatement ps=con.prepareStatement("SELECT price_per_day FROM bikes WHERE bike_id=?")){
                    ps.setInt(1,selected.id); try(ResultSet rs=ps.executeQuery()){
                        if(rs.next()) estimate.setText("Estimated total: "+rs.getBigDecimal(1).multiply(BigDecimal.valueOf(number)));
                    }
                }
            } catch(Exception ex){showError(this,"Cannot calculate estimate.",ex);}
        }

        private void rent() {
            Choice c=(Choice)customer.getSelectedItem(), b=(Choice)bike.getSelectedItem();
            if(c==null || b==null){info(this,"Add a customer and an available bike first.");return;}
            try {
                int number=Integer.parseInt(days.getText().trim());
                if(number<1 || number>365) throw new IllegalArgumentException("Rental duration must be between 1 and 365 days.");
                LocalDate today=LocalDate.now(), due=today.plusDays(number);
                try(Connection con=DB.getConnection()){
                    con.setAutoCommit(false);
                    try {
                        BigDecimal rate;
                        try(PreparedStatement lock=con.prepareStatement("SELECT price_per_day,status FROM bikes WHERE bike_id=? FOR UPDATE")){
                            lock.setInt(1,b.id); try(ResultSet rs=lock.executeQuery()){
                                if(!rs.next() || !"AVAILABLE".equals(rs.getString("status"))) throw new SQLException("That bike is no longer available.");
                                rate=rs.getBigDecimal("price_per_day");
                            }
                        }
                        BigDecimal total=rate.multiply(BigDecimal.valueOf(number));
                        try(PreparedStatement insert=con.prepareStatement(
                                "INSERT INTO rentals(customer_id,bike_id,rent_date,due_date,total_amount,status) VALUES(?,?,?,?,?,'ACTIVE')")){
                            insert.setInt(1,c.id);insert.setInt(2,b.id);insert.setDate(3,Date.valueOf(today));
                            insert.setDate(4,Date.valueOf(due));insert.setBigDecimal(5,total);insert.executeUpdate();
                        }
                        try(PreparedStatement update=con.prepareStatement("UPDATE bikes SET status='RENTED' WHERE bike_id=?")){
                            update.setInt(1,b.id);update.executeUpdate();
                        }
                        con.commit();
                        info(this,"Bike rented successfully.\nDue date: "+due+"\nEstimated charge: "+total);
                    } catch(Exception ex){con.rollback();throw ex;}
                    finally{con.setAutoCommit(true);}
                }
                days.setText(""); estimate.setText("Estimated total: -"); reloadAll();
            } catch(Exception ex){showError(this,"Could not rent bike.",ex);}
        }

        private void returnBike() {
            int row=grid.getSelectedRow();
            if(row<0){info(this,"Select an active rental from the table.");return;}
            int rentalId=(int)table.getValueAt(row,0);
            try(Connection con=DB.getConnection()){
                con.setAutoCommit(false);
                try {
                    int bikeId; LocalDate rentDate; BigDecimal rate;
                    try(PreparedStatement ps=con.prepareStatement("""
                        SELECT r.bike_id,r.rent_date,b.price_per_day,r.status
                        FROM rentals r JOIN bikes b ON b.bike_id=r.bike_id
                        WHERE r.rental_id=? FOR UPDATE
                        """)){
                        ps.setInt(1,rentalId);
                        try(ResultSet rs=ps.executeQuery()){
                            if(!rs.next() || !"ACTIVE".equals(rs.getString("status"))) throw new SQLException("Rental is already returned or missing.");
                            bikeId=rs.getInt("bike_id");rentDate=rs.getDate("rent_date").toLocalDate();rate=rs.getBigDecimal("price_per_day");
                        }
                    }
                    LocalDate returned=LocalDate.now();
                    long chargeDays=Math.max(1,ChronoUnit.DAYS.between(rentDate,returned));
                    BigDecimal total=rate.multiply(BigDecimal.valueOf(chargeDays));
                    try(PreparedStatement ps=con.prepareStatement("UPDATE rentals SET returned_date=?,total_amount=?,status='RETURNED' WHERE rental_id=?")){
                        ps.setDate(1,Date.valueOf(returned));ps.setBigDecimal(2,total);ps.setInt(3,rentalId);ps.executeUpdate();
                    }
                    try(PreparedStatement ps=con.prepareStatement("UPDATE bikes SET status='AVAILABLE' WHERE bike_id=?")){
                        ps.setInt(1,bikeId);ps.executeUpdate();
                    }
                    con.commit();
                    String invoice="BIKE RENTAL INVOICE\nRental ID: "+rentalId+"\nRent date: "+rentDate+
                            "\nReturn date: "+returned+"\nCharged days: "+chargeDays+
                            "\nRate per day: "+rate+"\nTOTAL: "+total;
                    JTextArea area=new JTextArea(invoice,10,32);area.setEditable(false);
                    JOptionPane.showMessageDialog(this,new JScrollPane(area),"Return receipt",JOptionPane.INFORMATION_MESSAGE);
                } catch(Exception ex){con.rollback();throw ex;}
                finally{con.setAutoCommit(true);}
            } catch(Exception ex){showError(this,"Could not return bike.",ex);}
            reloadAll();
        }

        private void reloadAll() {
            try{loadChoices();}catch(Exception ex){showError(this,"Could not load dropdowns.",ex);}
            table.setRowCount(0);
            try(Connection con=DB.getConnection(); Statement st=con.createStatement();
                ResultSet rs=st.executeQuery("""
                    SELECT r.rental_id,c.customer_name,b.bike_name,r.rent_date,r.due_date,
                           r.returned_date,r.total_amount,r.status
                    FROM rentals r JOIN customers c ON c.customer_id=r.customer_id
                    JOIN bikes b ON b.bike_id=r.bike_id ORDER BY r.rental_id DESC
                    """)){
                while(rs.next()) table.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),
                        rs.getDate(4),rs.getDate(5),rs.getDate(6),rs.getBigDecimal(7),rs.getString(8)});
            }catch(Exception ex){showError(this,"Could not load rental history.",ex);}
        }
    }

    static class Choice {
        final int id; final String label;
        Choice(int id,String label){this.id=id;this.label=label;}
        @Override public String toString(){return label;}
    }
}
