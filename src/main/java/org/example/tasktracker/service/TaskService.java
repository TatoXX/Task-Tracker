package org.example.tasktracker.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.tasktracker.model.Task;

import org.example.tasktracker.model.User;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class TaskService {
    private List<Task> tasks = new ArrayList<>();




    //GSON

    private final String filename = "data/tasks.json";

    public void saveTasksToFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(tasks, writer);
            System.out.println("Tasks saved successfully.");
        } catch (IOException e) {
            System.out.println("Failed to save tasks: " + e.getMessage());
        }
    }

    // Load tasks from JSON file
    public void loadTasksFromFile() {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No saved tasks found, starting fresh.");
            return;
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(filename)) {
            Type taskListType = new TypeToken<List<Task>>(){}.getType();
            tasks = gson.fromJson(reader, taskListType);

            // If you have IDs and want to continue counting properly
            long maxId = tasks.stream()
                    .mapToLong(Task::getId)
                    .max()
                    .orElse(0L);
            Task.setNextId(maxId + 1);

            System.out.println("Tasks loaded successfully.");
        } catch (IOException e) {
            System.out.println("Failed to load tasks: " + e.getMessage());
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


    public void updateTask(int id, Task updatedTask ) {
        Task foundTask = null;
        for (Task task : tasks) {
            if (task.getId() == id) {
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

    public void createTask(Task task) {
        task.setId((long) (tasks.size() + 1));  // simple incremental ID
        tasks.add(task);
    }

    public List<Task> getTasksByUser(Long userId) {
        // Create a new list where we will collect the matching tasks
        List<Task> userTasks = new ArrayList<>();

        // Go through each task in our tasks list
        for (Task task : tasks) {
            // Check if this task belongs to the given user
            if (task.getUser().getId().equals(userId)) {
                // Add it to the result list
                userTasks.add(task);
            }
        }

        // Return the list of tasks that belong to this user
        return userTasks;
    }
}

