package org.example.tasktracker.controller;

import org.example.tasktracker.model.User;
import org.example.tasktracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.mindrot.jbcrypt.BCrypt;

@Controller
public class AuthController {

    private final UserService userService;

    /**
     * Constructor-based dependency injection for UserService
     */
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ========================== LOGIN ==========================

    /**
     * GET /login
     * Show the login page.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * POST /login
     * Handle login form submission.
     * Checks if a user with the given username and password exists.
     * If valid, redirects to dashboard.
     * If invalid, stays on login page and shows an error.
     */
    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model) {

        User foundUser = null;

        // Loop through registered users to find a match
        for (User u : userService.getAllUsers()) {
            if (u.getName().equals(username) && BCrypt.checkpw(password, u.getPassword())) {
                foundUser = u;
                break;
            }
        }

        if (foundUser != null) {
            model.addAttribute("user", foundUser);
            return "dashboard/index"; // Successful login
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "auth/login"; // Back to login page with error
        }
    }

    // ========================== REGISTER ==========================

    /**
     * GET /register
     * Show the registration page.
     */
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    /**
     * POST /register
     * Handle registration form submission.
     * Adds the new user to the service.
     */
    @PostMapping("/register")
    public String handleRegister(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String email,
                                 Model model) {

        // 1️⃣ Validate name
        if (!userService.isValidName(username)) {
            model.addAttribute("error", "Invalid name. Must start with a capital letter, 2-30 characters.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "auth/register";
        }

        // 2️⃣ Validate email
        if (!userService.isValidEmail(email)) {
            model.addAttribute("error", "Invalid email format.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "auth/register";
        }

        // 3️⃣ Check if email is unique
        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "auth/register";
        }

        // 4️⃣ Validate password
        if (!userService.isValidPassword(password)) {
            model.addAttribute("error", "Password must be 8-20 characters, include one uppercase letter, one number, and one special character.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "auth/register";
        }

        // ✅ All validations passed, register user
        User user = new User(username, email, password);
        userService.registerUser(user);
        model.addAttribute("user", user);

        return "dashboard/index"; // Redirect to dashboard after successful registration
    }


    // ========================== FORGOT PASSWORD ==========================

    /**
     * GET /forgot-password
     * Show the forgot-password page.
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    /**
     * POST /forgot-password
     * Handle forgot-password form submission.
     * If email exists, show success message on login page.
     * If email does not exist, stay on forgot-password page with error.
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, Model model) {

        User user = userService.findByEmail(email);

        if (user != null) {
            // Simulate sending a reset link
            model.addAttribute("messageEmailPasswordReset", "Password reset link sent to " + email);
            return "auth/login"; // Show success on login page
        } else {
            model.addAttribute("errorEmailPasswordReset", "No user found with email: " + email);
            model.addAttribute("email", email); // Keep the input filled
            return "auth/forgot-password"; // Show error on forgot-password page
        }
    }
}
