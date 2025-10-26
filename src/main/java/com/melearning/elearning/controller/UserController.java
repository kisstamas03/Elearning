package com.melearning.elearning.controller;

import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        // Ellenőrzések
        if (result.hasErrors()) {
            return "register";
        }

        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "A felhasználónév már foglalt!");
            return "register";
        }

        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Ez az email cím már regisztrálva van!");
            return "register";
        }

        // Ha nincs beállítva szerepkör, alapértelmezetten STUDENT lesz
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }

        try {
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "Sikeres regisztráció! Most már bejelentkezhetsz.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Hiba történt a regisztráció során: " + e.getMessage());
            return "register";
        }
    }
}