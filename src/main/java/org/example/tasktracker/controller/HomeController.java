package org.example.tasktracker.controller;

import org.example.tasktracker.model.Task;
import org.example.tasktracker.model.User;
import org.example.tasktracker.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.Optional;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final TaskService taskService;

    @Autowired
    public HomeController(TaskService taskService) {
        this.taskService = taskService;
    }


    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");

        if (loggedUser == null) {
            return "redirect:/login"; // if not logged in, back to login
        }
        model.addAttribute("user", loggedUser);

        // Load tasks for this user
        List<Task> userTasks = taskService.getTasksByUser(loggedUser);
        model.addAttribute("tasks", userTasks);

        // === COUNTS ===
        long totalTasks = userTasks.size();
        long completedTasks = userTasks.stream().filter(Task::isCompleted).count();
        long pendingTasks = totalTasks - completedTasks;

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);

        return "dashboard/index"; // show dashboard
    }


    @PostMapping("/home/add-task")
    public String addTask(@RequestParam String title,
                          @RequestParam(required = false) String description,
                          HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";

        Task task = new Task(title ,description,loggedUser);
        // Add task for this user
        taskService.createTask(task);

        // Redirect back to home page to show updated task list
        return "redirect:/home";
    }

    @GetMapping("/home/delete-task/{id}")
    public String deleteTask(@PathVariable Long id) {
        Task task = taskService.findTaskById(id);
        if (task != null) {
            taskService.deleteTaskById(id);  // remove the task from your list
            taskService.saveTasksToFile(); // persist changes
        }
        return "redirect:/home";
    }


    @GetMapping("/home/edit-task/{id}")
    public String editTaskForm(@PathVariable Long id, Model model) {
        Task task = taskService.findTaskById(id);
        model.addAttribute("task", task);
        return "task/edit-task";
    }

    @PostMapping("/home/update-task/{id}")
    public String updateTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description) {
        Task task = taskService.findTaskById(id);
        task.setTitle(title);
        task.setDescription(description);
        taskService.saveTasksToFile();
        return "redirect:/home";
    }

    @GetMapping("/home/toggle-task/{id}")
    public String toggleTask(@PathVariable Long id) {
        Task task = taskService.findTaskById(id);

        // Flip completed flag
        task.setCompleted(!task.isCompleted());
        taskService.saveTasksToFile();

        return "redirect:/home";
    }









}
