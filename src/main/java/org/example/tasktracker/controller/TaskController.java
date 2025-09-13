package org.example.tasktracker.controller;


import org.example.tasktracker.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.example.tasktracker.model.Task;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping
    @ResponseBody
    public List<Task> getTasks() {
        return taskService.getAllTasks();
    }


    @PostMapping
    public String createTask(@RequestParam String title, @RequestParam(required = false, defaultValue = "false") boolean completed) {
        Task task = new Task();
        task.setTitle(title);
        task.setCompleted(completed);
        taskService.createTask(task);
        return "redirect:/tasks";  // Redirect browser to /tasks after POST
    }
    @PostMapping("/tasks")  // ← This catches requests to "/tasks" with POST method
    public String createTask(@RequestParam String title,        // ← Gets "title" from form
                             @RequestParam String description) { // ← Gets "description" from form
        // Your logic here
        return "redirect:/dashboard";
    }

}
