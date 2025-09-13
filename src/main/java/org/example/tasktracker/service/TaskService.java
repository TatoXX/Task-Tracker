package org.example.tasktracker.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.example.tasktracker.model.Task;

import org.example.tasktracker.model.User;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class TaskService {
    private List<Task> tasks = new ArrayList<>();


    //GSON

    private final String filename = "data/tasks.json";

    private Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }

// .............................................................................................

    /**
     * Save all tasks to JSON file
     */
    public void saveTasksToFile() {
        File file = new File(filename);
        file.getParentFile().mkdirs(); // Ensure directory exists
        try (Writer writer = new FileWriter(file)) {
            getGson().toJson(tasks, writer);
            System.out.println("✅ Tasks saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to save tasks: " + e.getMessage());
        }
    }

// .............................................................................................

    /**
     * Load tasks from JSON file
     */
    public void loadTasksFromFile() {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            System.out.println("⚠️ No saved tasks found or file empty, starting fresh. Expected file at: " + file.getAbsolutePath());
            tasks = new ArrayList<>();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Type taskListType = new TypeToken<List<Task>>() {}.getType();
            tasks = getGson().fromJson(reader, taskListType);

            if (tasks == null) {
                tasks = new ArrayList<>();
            }

            // Update next task ID
            long maxId = tasks.stream()
                    .mapToLong(Task::getId)
                    .max()
                    .orElse(0L);
            Task.setNextId(maxId + 1);

            System.out.println("✅ Tasks loaded successfully from: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to load tasks: " + e.getMessage());
        }
    }



    public void deleteTaskById(Long id) {
        Task deletedTask = null;
        for (Task task : tasks) {
            if(Objects.equals(task.getId(), id)){
                deletedTask = task;

                break;

            }
        }
        if (deletedTask != null) {
            tasks.remove(deletedTask);
            System.out.println("Task with id " + id + " deleted successfully.");
        } else {
            System.out.println("Provided Id is wrong or task is already deleted.");
        }


    }

    public Task findTaskById(Long id) {
        Task foundTask = null;
        for (Task task : tasks) {
            if (Objects.equals(task.getId(), id)) {
                foundTask = task;
                break;
            }
        }

        if (foundTask != null) {
            System.out.println(foundTask);


        } else {
            System.out.println("Task with id " + id + " not found.");

        }
        return foundTask;
    }


    public List<Task> getAllTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks to display.");
            return null;
        }
        for (Task task : tasks) {
            System.out.println(task);
        }
        return null;
    }


    public void updateTask(Long id, Task updatedTask ) {
        Task foundTask = null;
        for (Task task : tasks) {
            if (Objects.equals(task.getId(), id)) {
                foundTask = task;
                break;

            }
        }
        if (foundTask != null) {

            foundTask.setTitle(updatedTask.getTitle());
            foundTask.setDescription(updatedTask.getDescription());
            foundTask.setCreatedAt(updatedTask.getCreatedAt());
            foundTask.setUpdatedAt(updatedTask.getUpdatedAt());

        }else{
            System.out.println("Task with id " + id + " not found.");
        }

    }
    public void updateTaskFields(Long id, String title, String description) {
        Task foundTask = null;
        for (Task task : tasks) {
            if (Objects.equals(task.getId(), id)) {
                foundTask = task;
                break;
            }
        }

        if (foundTask != null) {
            foundTask.setTitle(title);
            foundTask.setDescription(description);
            foundTask.setUpdatedAt(LocalDateTime.now()); // fixed here
        } else {
            System.out.println("Task with id " + id + " not found.");
        }
    }



    public void createTask(Task task) {
        task.setId((long) (tasks.size() + 1));  // simple incremental ID
        tasks.add(task);
        saveTasksToFile();
    }

    public List<Task> getTasksByUser(User user) {
        // Load all tasks from JSON first
        loadTasksFromFile();

        // Filter tasks for this user
        return tasks.stream()
                .filter(task -> task.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }




}

