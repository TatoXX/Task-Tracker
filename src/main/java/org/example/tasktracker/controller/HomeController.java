package org.example.tasktracker.controller;

import org.example.tasktracker.model.Task;
import org.example.tasktracker.model.User;
import org.example.tasktracker.service.TaskService;
import org.example.tasktracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final TaskService taskService;
    private final UserService userService;

    @Autowired
    public HomeController(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";

        model.addAttribute("user", loggedUser);

        // Load tasks for this user
        List<Task> userTasks = taskService.getTasksByUser(loggedUser);
        model.addAttribute("tasks", userTasks);

        // Task counts
        model.addAttribute("totalTasks", taskService.getTotalTasksByUser(loggedUser));
        model.addAttribute("completedTasks", taskService.getCompletedTasksByUser(loggedUser));
        model.addAttribute("pendingTasks", taskService.getPendingTasksByUser(loggedUser));
        model.addAttribute("inProgressTasks", taskService.getInProgressTasksByUser(loggedUser));

        return "dashboard/index";
    }

    @PostMapping("/home/add-task")
    public String addTask(@RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String priority,
                          @RequestParam(required = false) String dueDate,
                          HttpSession session) {

        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";

        Task task = new Task(title, description, loggedUser);

        if (priority != null && !priority.isEmpty()) task.setPriority(priority);
        if (dueDate != null && !dueDate.isEmpty()) {
            try { task.setDueDate(LocalDate.parse(dueDate)); }
            catch (Exception e) { System.out.println("Invalid date format: " + dueDate); }
        }

        taskService.createTask(task);
        return "redirect:/home";
    }

    @GetMapping("/home/delete-task/{id}")
    public String deleteTask(@PathVariable Long id,HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";
        taskService.deleteTaskById(id, loggedUser);
        return "redirect:/home";
    }

    @GetMapping("/home/edit-task/{id}")
    public String editTaskForm(@PathVariable Long id, Model model,
                               HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";

        Task task = taskService.findTaskById(id, loggedUser);
        model.addAttribute("task", task);
        return "task/edit-task";
    }

    @PostMapping("/home/update-task/{id}")
    public String updateTask(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String dueDate,
                             HttpSession session) {


        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";
        Task task = taskService.findTaskById(id, loggedUser);
        if (task != null) {
            task.setTitle(title);
            task.setDescription(description);
            if (priority != null && !priority.isEmpty()) task.setPriority(priority);
            if (dueDate != null && !dueDate.isEmpty()) {
                try { task.setDueDate(LocalDate.parse(dueDate)); }
                catch (Exception e) { System.out.println("Invalid date format: " + dueDate); }
            }
            taskService.saveTasksToFile();
        }
        return "redirect:/home";
    }

    @GetMapping("/home/toggle-task/{id}")
    public String toggleTask(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";

        taskService.toggleTaskStatusForUser(id, loggedUser);
        return "redirect:/home";
    }

    @PostMapping("/home/set-in-progress/{id}")
    public String setTaskInProgress(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";
        taskService.setTaskInProgress(id, true, loggedUser);
        return "redirect:/home";
    }

    @PostMapping("/home/set-todo/{id}")
    public String setTaskTodo(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("user");
        if (loggedUser == null) return "redirect:/login";
        taskService.setTaskInProgress(id, false, loggedUser);
        return "redirect:/home";
    }
}
