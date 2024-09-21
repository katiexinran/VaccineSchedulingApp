package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;
import java.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if password is strong password
        if (!isStrongPassword(password)) {
            System.out.println("Password is weak! Please choose a stronger password.");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if password is strong password
        if (!isStrongPassword(password)) {
            System.out.println("Password is weak! Please choose a stronger password.");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // search_caregiver_schedule <date>
        // check if a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // check if the length of tokens is correct
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        // check if the second token is a valid date
        String dateStr = tokens[1];
        Date date;
        try {
            date = Date.valueOf(dateStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format! Please enter a date in the format yyyy-mm-dd.");
            return;
        }

        // perform the search query
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Query to retrieve caregivers and vaccines available for the given date
            String query = "SELECT C.Username, V.Name, V.Doses " +
                    "FROM Caregivers C, Availabilities A, Vaccines V " +
                    "WHERE C.Username = A.Username AND " +
                    "A.Time = ? " +
                    "ORDER BY C.Username";

            stmt = con.prepareStatement(query);
            stmt.setDate(1, date);
            rs = stmt.executeQuery();

            // Check if there are available caregivers for the given date
            if (!rs.isBeforeFirst()) {
                System.out.println("No available caregivers for the given date.");
                return;
            }

            // Print the results
            System.out.println("Available caregivers for " + dateStr + ":");
            while (rs.next()) {
                String caregiverUsername = rs.getString("Username");
                String vaccineName = rs.getString("Name");
                int availableDoses = rs.getInt("Doses");

                System.out.println(caregiverUsername + " " + vaccineName + " " + availableDoses);
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // reserve <date> <vaccine>

        // check if a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // check if a patient is logged in
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }

        // check if the length of tokens is correct
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        // check if the second token is a valid date
        String dateStr = tokens[1];
        Date date;
        try {
            date = Date.valueOf(dateStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format! Please enter a date in the format yyyy-mm-dd.");
            return;
        }

        // check if the third token is a valid vaccine name
        String vaccineName = tokens[2];
        if (!vaccineExists(vaccineName)) {
            System.out.println("Invalid vaccine name! Please enter a valid vaccine.");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Query to check if a caregiver is available for the given date and vaccine
            String query = "SELECT A.Username, V.Doses " +
                    "FROM Availabilities A, Vaccines V " +
                    "WHERE A.Username NOT IN " +
                    "(SELECT CaregiverUsername FROM Appointments WHERE Time = ?) " +
                    "AND A.Time = ? AND V.Name = ? " +
                    "ORDER BY A.Username";

            stmt = con.prepareStatement(query);
            stmt.setDate(1, date);
            stmt.setDate(2, date);
            stmt.setString(3, vaccineName);
            rs = stmt.executeQuery();

            // Check if a caregiver is available
            if (!rs.isBeforeFirst()) {
                System.out.println("No available caregiver for the given date and vaccine.");
                return;
            }

            // Check if there are enough vaccine doses available
            int availableDoses = 0;
            String caregiverUsername = "";
            while (rs.next()) {
                availableDoses = rs.getInt("Doses");
                if (availableDoses > 0) {
                    caregiverUsername = rs.getString("Username");
                    break;
                }
            }

            if (availableDoses <= 0) {
                System.out.println("Not enough available doses!");
                return;
            }

            // Generate a new appointment ID
            int appointmentID = generateAppointmentID();

            // Create the appointment
            String createAppointmentQuery = "INSERT INTO Appointments (AppointmentID, PatientUsername, CaregiverUsername, VaccineName, Time) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement createAppointmentStmt = con.prepareStatement(createAppointmentQuery);
            createAppointmentStmt.setInt(1, appointmentID);
            createAppointmentStmt.setString(2, currentPatient.getUsername());
            createAppointmentStmt.setString(3, caregiverUsername);
            createAppointmentStmt.setString(4, vaccineName);
            createAppointmentStmt.setDate(5, date);
            createAppointmentStmt.executeUpdate();

            // Decrease the available doses for the vaccine
            String updateVaccineQuery = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            PreparedStatement updateVaccineStmt = con.prepareStatement(updateVaccineQuery);
            updateVaccineStmt.setString(1, vaccineName);
            updateVaccineStmt.executeUpdate();

            // Remove the caregiver from availabilities for the specific date
            String removeAvailabilityQuery = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
            PreparedStatement removeAvailabilityStmt = con.prepareStatement(removeAvailabilityQuery);
            removeAvailabilityStmt.setString(1, caregiverUsername);
            removeAvailabilityStmt.setDate(2, date);
            removeAvailabilityStmt.executeUpdate();
         } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            // Close the result set, statement, and connection
            try {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean vaccineExists(String vaccineName) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Query to retrieve vaccine names
            String query = "SELECT COUNT(*) FROM Vaccines WHERE name = ?";
            stmt = con.prepareStatement(query);
            stmt.setString(1, vaccineName);
            rs = stmt.executeQuery();

            // Check if vaccine with given name exists
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            // Close the result set, statement, and connection
            try {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static int generateAppointmentID() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i=1; i<11; i++) list.add(i);
        Collections.shuffle(list);
        return list.get(0);
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // Check if a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query;
            if (currentPatient != null) {
                // Query for patient's appointments
                query = "SELECT A.AppointmentID, A.VaccineName, A.Time, C.Username " +
                        "FROM Appointments A, Caregivers C " +
                        "WHERE A.CaregiverUsername = C.Username AND " +
                        "A.PatientUsername = ? " +
                        "ORDER BY A.AppointmentID";
                stmt = con.prepareStatement(query);
                stmt.setString(1, currentPatient.getUsername());
            } else if (currentCaregiver != null) {
                // Query for caregiver's appointments
                query = "SELECT A.AppointmentID, A.VaccineName, A.Time, P.Username " +
                        "FROM Appointments A, Patients P " +
                        "WHERE A.PatientUsername = P.Username AND " +
                        "A.CaregiverUsername = ? " +
                        "ORDER BY A.AppointmentID";
                stmt = con.prepareStatement(query);
                stmt.setString(1, currentCaregiver.getUsername());
            }

            if (stmt != null) {
                rs = stmt.executeQuery();

                // Check if any appointments are found
                if (!rs.isBeforeFirst()) {
                    System.out.println("No appointments found for the current user.");
                    return;
                }

                // Print the appointments
                System.out.println("Appointments:");

                while (rs.next()) {
                    int appointmentID = rs.getInt("AppointmentID");
                    String vaccineName = rs.getString("VaccineName");
                    Date date = rs.getDate("Time");
                    String otherUserName = rs.getString(4);

                    System.out.println("Appointment ID: " + appointmentID);
                    System.out.println("Vaccine: " + vaccineName);
                    System.out.println("Date: " + date);

                    if (currentPatient != null) {
                        System.out.println("Caregiver: " + otherUserName);
                    } else if (currentCaregiver != null) {
                        System.out.println("Patient: " + otherUserName);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // check if a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("No user logged in.");
            return;
        }

        // log out the current user
        try {
            if (currentCaregiver != null) {
                currentCaregiver = null;
            } else if (currentPatient != null) {
                currentPatient = null;
            }
            System.out.println("Successfully logged out!");
        } catch (Exception e) {
            System.out.println("Please try again!");
        }
    }

    private static boolean isStrongPassword(String password) {
        return password.length() >= 8 && password.matches(".*[A-Z].*") && password.matches(".*[a-z].*") && password.matches(".*[0-9].*") && password.matches(".*[!@#?].*");
    }
}
