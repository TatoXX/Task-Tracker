package org.example.tasktracker.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.example.tasktracker.model.Task;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @GetMapping
    public List<Task> getTasks() {
        return Arrays.asList(
                new Task(1L, "Buy groceries", false),
                new Task(2L, "Finish Java assignment", true),
                new Task(3L, "Walk the dog", false)
        );
    }
}
