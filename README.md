# Bike Rental Management System (Java Swing + JDBC + MySQL)

A desktop application built for Java 17, Swing, Maven and MySQL. It includes:
- Admin login with PBKDF2 password hashing
- Bike create, read, update and delete
- Customer create, read, update and delete
- Bike rental with availability checking and estimated billing
- Bike return, final charge calculation and receipt
- Rental history

## Requirements
1. JDK 17 or newer
2. Visual Studio Code
3. VS Code extensions: Extension Pack for Java and Maven for Java
4. MySQL Server 8.0+ (or compatible MySQL server)
5. Internet access for Maven to download MySQL Connector/J the first time

## 1. Create the MySQL database

Run this in MySQL Workbench or the MySQL command line:

```sql
CREATE DATABASE bike_rental;
```

The application creates its tables automatically when it starts.

## 2. Configure database credentials

The default connection is:
- URL: `jdbc:mysql://localhost:3306/bike_rental`
- User: `root`
- Password: `your_mysql_password`

Set your actual MySQL password before launching.

**Windows PowerShell**
```powershell
$env:BIKE_DB_USER="root"
$env:BIKE_DB_PASSWORD="YOUR_MYSQL_PASSWORD"
$env:BIKE_DB_URL="jdbc:mysql://localhost:3306/bike_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

**macOS / Linux**
```bash
export BIKE_DB_USER=root
export BIKE_DB_PASSWORD='YOUR_MYSQL_PASSWORD'
export BIKE_DB_URL='jdbc:mysql://localhost:3306/bike_rental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

Alternatively, edit the fallback values in `DB.java`. Environment variables are recommended so passwords are not committed to source control.

## 3. Open in VS Code

1. Extract the ZIP file.
2. In VS Code, choose **File > Open Folder** and select `BikeRentalSystem`.
3. Install the recommended Java and Maven extensions if prompted.
4. Wait for Maven dependencies to finish downloading.
5. Ensure the VS Code Java runtime is JDK 17 or newer.

## 4. Run

Open the VS Code terminal in the project directory and run:

```bash
mvn clean compile
mvn exec:java
```

You can also open `Main.java` and use the **Run** action provided by the Java extension after Maven has imported the project.

## 5. First login

On first startup, the app creates a default administrator if one does not already exist:

- Username: `admin`
- Password: `admin123`

Change this default password before using the application with real customer information. The stored password is PBKDF2-hashed, not stored as plain text. The starter project does not yet include an in-app password-change screen.

## How to use

1. **Bikes**: Add a bike with its name, type and per-day rate. Select a row to update or delete it. Only available bikes can be updated/deleted.
2. **Customers**: Add customers with name and phone; email is optional. Select a row to update/delete.
3. **Rentals & Returns**: Choose a customer and an available bike, enter the rental duration in days, then use Estimate or Rent bike.
4. **Return**: Select an `ACTIVE` rental row and click Return selected. The application calculates the final amount using elapsed calendar days, with a minimum of one billable day, marks the rental returned, makes the bike available and displays a receipt.
5. **Rental history**: All active and returned rentals remain in the table.

## Billing rule

- Rental estimate = daily rate × selected rental days.
- Final return charge = daily rate × elapsed calendar days from rental date to return date.
- A same-day rental is charged for at least one day.
- This starter version does not include late fees, deposits, taxes or partial-day billing.

## Project layout

```text
BikeRentalSystem/
├── pom.xml
├── README.md
├── sql/schema.sql
└── src/main/java/com/bikerental/
    ├── DB.java
    ├── Security.java
    └── Main.java
```

## Troubleshooting

- **Communications link failure**: Start MySQL Server and verify the host/port in `BIKE_DB_URL`.
- **Access denied for user**: Set `BIKE_DB_USER` and `BIKE_DB_PASSWORD` correctly.
- **Unknown database**: Run `CREATE DATABASE bike_rental;`.
- **No suitable driver**: Run from Maven (`mvn exec:java`) and allow Maven to download dependencies.
- **Java version error**: Use JDK 17+ and check `java -version` and `mvn -version`.

## Notes for production use

This is a learning/project starter, not a hardened production deployment. Add role-based access, password-change/reset, audit logs, backups, input validation appropriate to local rules, and stronger operational database permissions before real-world use. Use TLS for database connections outside localhost.
