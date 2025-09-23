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
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public String showTaskManagement(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) String priority,
                                     @RequestParam(required = false) String sortBy,
                                     HttpSession session, Model model) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        // Get all tasks for the user
        List<Task> userTasks = taskService.getTasksByUser(loggedUser);

        // Filter by priority if provided
        if (priority != null && !priority.isEmpty()) {
            userTasks.removeIf(task -> task.getPriority() == null || !task.getPriority().equalsIgnoreCase(priority));
        }

        // Filter by search term if provided
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            userTasks.removeIf(task ->
                    (task.getTitle() == null || !task.getTitle().toLowerCase().contains(lowerSearch)) &&
                            (task.getDescription() == null || !task.getDescription().toLowerCase().contains(lowerSearch))
            );
        }

        // Sorting
        if (sortBy != null) {
            switch (sortBy) {
                case "title":
                    userTasks.sort(Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "priority":
                    userTasks.sort(Comparator.comparingInt(task -> {
                        if ("high".equalsIgnoreCase(task.getPriority())) return 1;
                        if ("medium".equalsIgnoreCase(task.getPriority())) return 2;
                        if ("low".equalsIgnoreCase(task.getPriority())) return 3;
                        return 4; // default for null or unknown
                    }));
                    break;
                case "due_date":
                    userTasks.sort(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(LocalDate::compareTo)));
                    break;
                case "created":
                default:
                    userTasks.sort(Comparator.comparing(
                            task -> task.getCreatedAt(),
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ));
            }
        }

        // Pass filtered tasks to view
        model.addAttribute("tasks", userTasks);
        model.addAttribute("search", search);
        model.addAttribute("priority", priority);
        model.addAttribute("sortBy", sortBy);

        // --- COUNTS FOR THYMELEAF ---
        long completedCount = userTasks.stream().filter(Task::isCompleted).count();
        long inProgressCount = userTasks.stream().filter(Task::isInProgress).count();
        long todoCount = userTasks.stream().filter(t -> !t.isCompleted() && !t.isInProgress()).count();

        model.addAttribute("completedCount", completedCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("todoCount", todoCount);
        // ----------------------------

        // Projects list (empty for now)
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
            Task task = taskService.findTaskById(id, loggedUser);
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

    // --- REST OF THE CONTROLLER REMAINS UNCHANGED ---
    @GetMapping("/edit/{id}")
    public String editTaskForm(@PathVariable Long id, HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        Task task = taskService.findTaskById(id, loggedUser);
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
            Task task = taskService.findTaskById(id, loggedUser);
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

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public String toggleTask(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) {
            return "error";
        }

        try {
            Task task = taskService.findTaskById(id, loggedUser);
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

    @GetMapping("/all")
    @ResponseBody
    public List<Task> getTasks() {
        return taskService.getAllTasks();
    }
}
