package org.example.tasktracker.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.example.tasktracker.model.Role;
import org.example.tasktracker.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    // List to store all users in memory
    private List<User> users = new ArrayList<>();

    // File where users are persisted
    private final String filename = "data/users.json";

    // Default role assigned to newly registered users
    private static final Role DEFAULT_ROLE = new Role("ROLE_USER");

    /**
     * Initialize service by loading users from JSON file
     */
    @PostConstruct
    public void init() {
        loadUsersFromFile();
    }

    //  .............................................................................................

    /**
     * Create Gson instance that can handle LocalDateTime serialization/deserialization
     */
    private Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }

    //  .............................................................................................

    /**
     * Save all users to JSON file
     */
    public void saveUsersToFile() {
        File file = new File(filename);
        file.getParentFile().mkdirs(); // Ensure directory exists
        try (Writer writer = new FileWriter(file)) {
            getGson().toJson(users, writer);
            System.out.println("✅ Users saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to save users: " + e.getMessage());
        }
    }

    //  .............................................................................................

    /**
     * Load users from JSON file
     */
    public void loadUsersFromFile() {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            System.out.println("⚠️ No saved users found or file empty, starting fresh. Expected file at: " + file.getAbsolutePath());
            users = new ArrayList<>();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Type userListType = new TypeToken<List<User>>() {}.getType();
            users = getGson().fromJson(reader, userListType);

            if (users == null) {
                users = new ArrayList<>();
            }

            // Update next user ID
            long maxId = users.stream()
                    .mapToLong(User::getId)
                    .max()
                    .orElse(0L);
            User.setNextId(maxId + 1);

            System.out.println("✅ Users loaded successfully from: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to load users: " + e.getMessage());
        }
    }

    //  .............................................................................................

    /**
     * Register a new user
     * Hashes the password and assigns default role
     */
    public void registerUser(User user) {
        user.setId((long) (users.size() + 1)); // Assign next ID
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()); // Hash password
        user.setPassword(hashedPassword);
        user.getRoles().add(DEFAULT_ROLE); // Assign default role
        users.add(user);
        saveUsersToFile(); // Persist users
    }

    //  .............................................................................................

    /**
     * Get all registered users
     */
    public List<User> getAllUsers() {
        if (users.isEmpty()) {
            System.out.println("No Users to display.");
        }
        return users;
    }

    //  .............................................................................................

    /**
     * Find a user by ID
     */
    public User findUserById(Long id) {
        return users.stream()
                .filter(u -> Objects.equals(u.getId(), id))
                .findFirst()
                .orElse(null);
    }

    //  .............................................................................................

    /**
     * Find a user by email
     */
    public User findByEmail(String email) {
        return users.stream()
                .filter(u -> Objects.equals(u.getEmail(), email))
                .findFirst()
                .orElse(null);
    }

    //  .............................................................................................

    /**
     * Update existing user
     */
    public void updateUser(Long id, User updatedUser) {
        User foundUser = findUserById(id);
        if (foundUser != null) {
            foundUser.setName(updatedUser.getName());
            foundUser.setEmail(updatedUser.getEmail());
            foundUser.setPassword(updatedUser.getPassword());
            foundUser.setRoles(updatedUser.getRoles());
            saveUsersToFile();
        } else {
            System.out.println("User with id " + id + " not found.");
        }
    }

    //  .............................................................................................

    /**
     * Delete a user by ID
     */
    public void deleteUserById(Long id) {
        users.removeIf(u -> Objects.equals(u.getId(), id));
        saveUsersToFile();
    }

    //  .............................................................................................

    /**
     * Validate username (2-30 chars, starts with uppercase)
     */
    public static boolean isValidName(String name) {
        if (name == null) return false;
        if(name.length() < 2 || name.length() > 30) {
            return false;
        }
        return name.matches("^[A-Z][a-zA-Z]*(?:[ '-][a-zA-Z]+)*$");
    }

    //  .............................................................................................

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty() || email.length() > 254) return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    //  .............................................................................................

    /**
     * Validate password
     * Must have 8-20 chars, at least one uppercase, lowercase, digit, special char
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$");
    }

    //  .............................................................................................

    /**
     * Check if email is already taken
     */
    public boolean isEmailTaken(String email) {
        for (User u : getAllUsers()) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                return true; // Email exists
            }
        }
        return false; // Email is free
    }
}
