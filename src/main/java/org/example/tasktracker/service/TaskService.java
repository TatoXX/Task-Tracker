package org.example.tasktracker.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.example.tasktracker.model.Task;
import org.example.tasktracker.model.User;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private List<Task> tasks = new ArrayList<>();
    private final String filename = "data/tasks.json";

    @PostConstruct
    public void init() {
        loadTasksFromFile();  // ✅ only once, when app starts
    }

    private Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                        LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();
    }

    // Save all tasks to JSON file
    public void saveTasksToFile() {
        File file = new File(filename);
        file.getParentFile().mkdirs();

        try (Writer writer = new FileWriter(file)) {
            getGson().toJson(tasks, writer);
            System.out.println("✅ Tasks saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to save tasks: " + e.getMessage());
        }
    }

    // Load tasks from JSON file (only once at startup)
    private void loadTasksFromFile() {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            System.out.println("⚠️ No saved tasks found or file empty, starting fresh. Expected file at: " + file.getAbsolutePath());
            tasks = new ArrayList<>();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Type taskListType = new TypeToken<List<Task>>() {}.getType();
            List<Task> loadedTasks = getGson().fromJson(reader, taskListType);

            tasks = (loadedTasks != null) ? loadedTasks : new ArrayList<>();

            // Update next task ID
            long maxId = tasks.stream()
                    .mapToLong(Task::getId)
                    .max()
                    .orElse(0L);
            Task.setNextId(maxId + 1);

            System.out.println("✅ Tasks loaded successfully from: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("❌ Failed to load tasks: " + e.getMessage());
            System.out.println("Starting with empty task list.");
            tasks = new ArrayList<>();
        }
    }

    // CRUD operations
    public void createTask(Task task) {
        task.setId((long) (tasks.size() + 1)); // simple incremental ID
        task.setCreatedAt(LocalDateTime.now());
        tasks.add(task);
        saveTasksToFile();
    }

    public void deleteTaskById(Long id, User user) {
        Task deletedTask = findTaskById(id, user);

        if (deletedTask != null) {
            tasks.remove(deletedTask);
            saveTasksToFile();
            System.out.println("Task with id " + id + " deleted successfully.");
        } else {
            System.out.println("Provided Id is wrong or task is already deleted.");
        }
    }

    public Task findTaskById(Long id, User user) {
        return tasks.stream()
                .filter(task -> Objects.equals(task.getId(), id) && Objects.equals(task.getUser().getId(), user.getId()))
                .findFirst()
                .orElse(null);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public void updateTask(Long id, Task updatedTask, User user) {
        Task foundTask = findTaskById(id, user);
        if (foundTask != null) {
            foundTask.setTitle(updatedTask.getTitle());
            foundTask.setDescription(updatedTask.getDescription());
            foundTask.setUpdatedAt(LocalDateTime.now());
            foundTask.setCompleted(updatedTask.isCompleted());
            foundTask.setInProgress(updatedTask.isInProgress());
            foundTask.setPriority(updatedTask.getPriority());
            foundTask.setDueDate(updatedTask.getDueDate());
            saveTasksToFile();
        }
    }

    public void updateTaskFields(Long id, String title, String description, User user) {
        Task foundTask = findTaskById(id, user);
        if (foundTask != null) {
            foundTask.setTitle(title);
            foundTask.setDescription(description);
            foundTask.setUpdatedAt(LocalDateTime.now());
            saveTasksToFile();
        }
    }

    // ✅ No more loadTasksFromFile() here
    public List<Task> getTasksByUser(User user) {
        return tasks.stream()
                .filter(task -> task.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }

    // Counters
    public long getTotalTasksByUser(User user) {
        return getTasksByUser(user).size();
    }

    public long getCompletedTasksByUser(User user) {
        return getTasksByUser(user).stream()
                .filter(Task::isCompleted)
                .count();
    }

    public long getPendingTasksByUser(User user) {
        return getTasksByUser(user).stream()
                .filter(task -> !task.isCompleted() && !task.isInProgress())
                .count();
    }

    public long getInProgressTasksByUser(User user) {
        return getTasksByUser(user).stream()
                .filter(Task::isInProgress)
                .count();
    }

    // Status management
    public void toggleTaskStatusForUser(Long id, User user) {
        System.out.println("🔍 toggleTaskStatus called with id: " + id);
        Task task = findTaskById(id,user);
        if (task != null) {
            System.out.println("📝 Found task: " + task.getTitle());
            System.out.println("📝 Description: " + task.getDescription());
            System.out.println("📝 Before toggle - Completed: " + task.isCompleted());

            task.setCompleted(!task.isCompleted());

            System.out.println("📝 After toggle - Completed: " + task.isCompleted());
            saveTasksToFile();
            System.out.println("💾 saveTasksToFile() called");
        } else {
            System.out.println("❌ Task with id " + id + " not found!");
        }
    }

    public void setTaskInProgress(Long id, boolean inProgress, User user) {
        Task task = findTaskById(id,user);
        if (task != null) {
            task.setInProgress(inProgress);
            if (inProgress) {
                task.setCompleted(false);
            }
            saveTasksToFile();
        }
    }
}
