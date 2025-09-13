package org.example.tasktracker.controller;

import org.example.tasktracker.model.User;
import org.example.tasktracker.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ========================== LOGIN ==========================
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User foundUser = null;

        for (User u : userService.getAllUsers()) {
            if (u.getName().equals(username) && BCrypt.checkpw(password, u.getPassword())) {
                foundUser = u;
                break;
            }
        }

        if (foundUser != null) {
            session.setAttribute("user", foundUser);
            return "redirect:/home"; // ✅ go to dashboard
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password");
            return "redirect:/login";
        }
    }

    // ========================== REGISTER ==========================
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String email,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        // 1️⃣ Validate name
        if (!userService.isValidName(username)) {
            redirectAttributes.addFlashAttribute("error", "Invalid name. Must start with a capital letter, 2-30 characters.");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }

        // 2️⃣ Validate email
        if (!userService.isValidEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Invalid email format.");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }

        // 3️⃣ Check if email is unique
        if (userService.isEmailTaken(email)) {
            redirectAttributes.addFlashAttribute("error", "Email is already registered.");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }

        // 4️⃣ Validate password
        if (!userService.isValidPassword(password)) {
            redirectAttributes.addFlashAttribute("error", "Password must be 8-20 characters, include one uppercase letter, one number, and one special character.");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }

        // ✅ All validations passed, register user
        User user = new User(username, email, password);
        userService.registerUser(user);

        session.setAttribute("user", user); // auto-login after registration
        return "redirect:/home";
    }

    // ========================== LOGOUT ==========================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // clear session
        return "redirect:/login";
    }

    // ========================== FORGOT PASSWORD ==========================
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(email);

        if (user != null) {
            redirectAttributes.addFlashAttribute("messageEmailPasswordReset",
                    "Password reset link sent to " + email);
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorEmailPasswordReset",
                    "No user found with email: " + email);
            return "redirect:/forgot-password";
        }
    }
}
