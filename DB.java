package com.bikerental;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DB {
    private DB() {}

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        String url = env("BIKE_DB_URL",
                "jdbc:mysql://localhost:3306/bike_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = env("BIKE_DB_USER", "root");
        String password = env("BIKE_DB_PASSWORD", "SQL Password");
        return DriverManager.getConnection(url, user, password);
    }

    public static void initialize() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS admins (
                    admin_id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bikes (
                    bike_id INT PRIMARY KEY AUTO_INCREMENT,
                    bike_name VARCHAR(100) NOT NULL,
                    bike_type VARCHAR(50) NOT NULL,
                    price_per_day DECIMAL(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                    CONSTRAINT chk_bike_price CHECK (price_per_day >= 0),
                    CONSTRAINT chk_bike_status CHECK (status IN ('AVAILABLE','RENTED'))
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_name VARCHAR(100) NOT NULL,
                    phone VARCHAR(25) NOT NULL,
                    email VARCHAR(120)
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rentals (
                    rental_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_id INT NOT NULL,
                    bike_id INT NOT NULL,
                    rent_date DATE NOT NULL,
                    due_date DATE NOT NULL,
                    returned_date DATE NULL,
                    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    CONSTRAINT fk_rental_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id),
                    CONSTRAINT fk_rental_bike FOREIGN KEY (bike_id)
                        REFERENCES bikes(bike_id),
                    CONSTRAINT chk_rental_status CHECK (status IN ('ACTIVE','RETURNED'))
                )
                """);
        }
        ensureDefaultAdmin();
    }

    private static void ensureDefaultAdmin() throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement check = con.prepareStatement(
                     "SELECT admin_id FROM admins WHERE username = ?")) {
            check.setString(1, "admin");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return;
            }
            try (PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO admins(username,password_hash) VALUES(?,?)")) {
                insert.setString(1, "admin");
                insert.setString(2, Security.hashPassword("admin123"));
                insert.executeUpdate();
            }
        }
    }
}
