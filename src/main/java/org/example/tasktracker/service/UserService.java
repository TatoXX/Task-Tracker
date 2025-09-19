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
import java.time.LocalDate;
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

    // .............................................................................................

    /**
     * Create Gson instance with custom adapters for LocalDateTime and LocalDate
     */
    private Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
    }

    /**
     * Custom adapter for LocalDateTime
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    /**
     * Custom adapter for LocalDate
     */
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }

    // .............................................................................................

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
            e.printStackTrace();
        }
    }

    // .............................................................................................

    /**
     * Load users from JSON file with proper error handling
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
            List<User> loadedUsers = getGson().fromJson(reader, userListType);

            if (loadedUsers != null) {
                users = loadedUsers;
                System.out.println("✅ Users loaded successfully from: " + file.getAbsolutePath());
            } else {
                users = new ArrayList<>();
                System.out.println("⚠️ No users found in file, starting with empty list.");
            }

            // Update next user ID
            long maxId = users.stream()
                    .mapToLong(User::getId)
                    .max()
                    .orElse(0L);
            User.setNextId(maxId + 1);

        } catch (Exception e) {
            System.out.println("❌ Failed to load users: " + e.getMessage());
            System.out.println("🔄 Starting with empty user list. If this persists, try deleting the data/users.json file.");

            // If there's corruption, backup the file and start fresh
            try {
                File backupFile = new File(filename + ".backup");
                if (file.renameTo(backupFile)) {
                    System.out.println("📁 Corrupted file backed up to: " + backupFile.getAbsolutePath());
                }
            } catch (Exception backupException) {
                System.out.println("⚠️ Could not backup corrupted file: " + backupException.getMessage());
            }

            users = new ArrayList<>();
        }
    }

    // .............................................................................................

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

    // .............................................................................................

    /**
     * Get all registered users
     */
    public List<User> getAllUsers() {
        if (users.isEmpty()) {
            System.out.println("No Users to display.");
        }
        return users;
    }

    // .............................................................................................

    /**
     * Find a user by ID
     */
    public User findUserById(Long id) {
        return users.stream()
                .filter(u -> Objects.equals(u.getId(), id))
                .findFirst()
                .orElse(null);
    }

    // .............................................................................................

    /**
     * Find a user by email
     */
    public User findByEmail(String email) {
        return users.stream()
                .filter(u -> Objects.equals(u.getEmail(), email))
                .findFirst()
                .orElse(null);
    }

    // .............................................................................................

    /**
     * Update existing user
     */
    public void updateUser(Long id, User updatedUser) {
        User foundUser = findUserById(id);
        if (foundUser != null) {
            foundUser.setName(updatedUser.getName());
            foundUser.setEmail(updatedUser.getEmail());
            // Only update password if it's provided and different
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                foundUser.setPassword(updatedUser.getPassword());
            }
            if (updatedUser.getRoles() != null) {
                foundUser.setRoles(updatedUser.getRoles());
            }
            saveUsersToFile();
        } else {
            System.out.println("User with id " + id + " not found.");
        }
    }

    // .............................................................................................

    /**
     * Delete a user by ID
     */
    public void deleteUserById(Long id) {
        users.removeIf(u -> Objects.equals(u.getId(), id));
        saveUsersToFile();
    }

    // .............................................................................................

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

    // .............................................................................................

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty() || email.length() > 254) return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // .............................................................................................

    /**
     * Validate password
     * Must have 8-20 chars, at least one uppercase, lowercase, digit, special char
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$");
    }

    // .............................................................................................

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