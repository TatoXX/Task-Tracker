package org.example.tasktracker.controller;

import org.example.tasktracker.model.Task;
import org.example.tasktracker.model.User;
import org.example.tasktracker.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public String showTaskManagement(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        // Get all tasks for the user
        List<Task> userTasks = taskService.getTasksByUser(loggedUser);

        model.addAttribute("user", loggedUser);
        model.addAttribute("tasks", userTasks);

        // Add task statistics
        model.addAttribute("totalTasks", taskService.getTotalTasksByUser(loggedUser));
        model.addAttribute("completedTasks", taskService.getCompletedTasksByUser(loggedUser));
        model.addAttribute("pendingTasks", taskService.getPendingTasksByUser(loggedUser));
        model.addAttribute("inProgressTasks", taskService.getInProgressTasksByUser(loggedUser));

        // Add empty projects list for now (you can implement this later)
        model.addAttribute("projects", List.of());

        return "task/task-management";
    }

    @PostMapping("/add")
    public String addTask(@RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String priority,
                          @RequestParam(required = false) String dueDate,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            Task task = new Task(title, description, loggedUser);

            // Set priority (default to low if not provided)
            if (priority != null && !priority.isEmpty()) {
                task.setPriority(priority);
            }

            // Set due date if provided
            if (dueDate != null && !dueDate.isEmpty()) {
                try {
                    task.setDueDate(LocalDate.parse(dueDate));
                } catch (Exception e) {
                    System.out.println("Invalid date format: " + dueDate);
                }
            }

            taskService.createTask(task);
            redirectAttributes.addFlashAttribute("success", "Task added successfully");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add task");
            e.printStackTrace();
        }

        return "redirect:/tasks";
    }

    @PostMapping("/update-status/{id}")
    public String updateTaskStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            Task task = taskService.findTaskById(id,loggedUser);
            if (task != null && task.getUser().getId().equals(loggedUser.getId())) {
                switch (status.toLowerCase()) {
                    case "todo":
                        task.setCompleted(false);
                        task.setInProgress(false);
                        break;
                    case "in_progress":
                        task.setCompleted(false);
                        task.setInProgress(true);
                        break;
                    case "completed":
                        task.setCompleted(true);
                        task.setInProgress(false);
                        break;
                }
                taskService.saveTasksToFile();
                redirectAttributes.addFlashAttribute("success", "Task status updated");
            } else {
                redirectAttributes.addFlashAttribute("error", "Task not found or access denied");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update task status");
            e.printStackTrace();
        }

        return "redirect:/tasks";
    }

    @GetMapping("/edit/{id}")
    public String editTaskForm(@PathVariable Long id, HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        Task task = taskService.findTaskById(id,loggedUser);
        if (task != null && task.getUser().getId().equals(loggedUser.getId())) {
            model.addAttribute("task", task);
            model.addAttribute("user", loggedUser);
            return "task/edit-task";
        }

        return "redirect:/tasks";
    }

    @PostMapping("/update/{id}")
    public String updateTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String dueDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            Task task = taskService.findTaskById(id,loggedUser);
            if (task != null && task.getUser().getId().equals(loggedUser.getId())) {
                task.setTitle(title);
                task.setDescription(description);

                if (priority != null && !priority.isEmpty()) {
                    task.setPriority(priority);
                }

                if (dueDate != null && !dueDate.isEmpty()) {
                    try {
                        task.setDueDate(LocalDate.parse(dueDate));
                    } catch (Exception e) {
                        System.out.println("Invalid date format: " + dueDate);
                    }
                }

                taskService.saveTasksToFile();
                redirectAttributes.addFlashAttribute("success", "Task updated successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Task not found or access denied");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update task");
            e.printStackTrace();
        }

        return "redirect:/tasks";
    }

    @PostMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            Task task = taskService.findTaskById(id, loggedUser);
            if (task != null && task.getUser().getId().equals(loggedUser.getId())) {
                taskService.deleteTaskById(id, loggedUser);
                redirectAttributes.addFlashAttribute("success", "Task deleted successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Task not found or access denied");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete task");
            e.printStackTrace();
        }

        return "redirect:/tasks";
    }

    @GetMapping("/delete/{id}")
    public String deleteTaskGet(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        return deleteTask(id, session, redirectAttributes);
    }

    // API endpoints for AJAX calls (if needed)
    @PostMapping("/toggle/{id}")
    @ResponseBody
    public String toggleTask(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "error";
        }

        try {
            Task task = taskService.findTaskById(id,loggedUser);
            if (task != null && task.getUser().getId().equals(loggedUser.getId())) {
                task.setCompleted(!task.isCompleted());
                taskService.saveTasksToFile();
                return "success";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    // Keep your existing method for compatibility
    @PostMapping("/tasks")
    public String createTask(@RequestParam String title,
                             @RequestParam String description,
                             HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        Task task = new Task(title, description, loggedUser);
        taskService.createTask(task);
        return "redirect:/tasks";
    }

    // Keep your existing method for compatibility
    @GetMapping("/all")
    @ResponseBody
    public List<Task> getTasks() {
        return taskService.getAllTasks();
    }
}