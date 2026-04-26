package com.melearning.elearning.controller;

import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.EmailService;
import com.melearning.elearning.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Segédmetódus a bejelentkezett felhasználó lekéréséhez
    private Optional<User> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return userService.getUserByUsername(auth.getName());
    }

    // ==========================================
    // PROFIL MEGJELENÍTÉSE
    // ==========================================
    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", userOpt.get());
        return "profile";
    }

    // ==========================================
    // 1. FELHASZNÁLÓNÉV MÓDOSÍTÁSA
    // ==========================================
    @PostMapping("/profile/update-username")
    public String updateUsername(
            @RequestParam String username,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        Optional<User> userOpt = getCurrentUser(auth);
        if (userOpt.isEmpty()) return "redirect:/login";
        User currentUser = userOpt.get();

        if (currentUser.getUsername().equals(username)) {
            return "redirect:/profile"; // Ha nem változott, nem csinálunk semmit
        }

        if (userService.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("errorUsername", "Ez a felhasználónév már foglalt!");
            return "redirect:/profile";
        }

        Optional<User> dbUserOpt = userService.getUserById(currentUser.getId());
        if (dbUserOpt.isPresent()) {
            User dbUser = dbUserOpt.get();
            dbUser.setUsername(username);
            userService.updateUser(dbUser);
        }

        emailService.sendUsernameChangeEmail(currentUser.getEmail(), currentUser.getFirstName(), username);

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("success", "A felhasználónév megváltozott. Kérlek, jelentkezz be újra!");
        return "redirect:/login";
    }

    // ==========================================
    // 2. EMAIL CÍM MÓDOSÍTÁSA
    // ==========================================
    @PostMapping("/profile/update-email")
    public String updateEmail(
            @RequestParam String email,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = getCurrentUser(auth);
        if (userOpt.isEmpty()) return "redirect:/login";
        User currentUser = userOpt.get();

        if (currentUser.getEmail().equals(email)) {
            return "redirect:/profile";
        }

        if (userService.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("errorEmail", "Ez az email cím már foglalt!");
            return "redirect:/profile";
        }

        Optional<User> dbUserOpt = userService.getUserById(currentUser.getId());
        if (dbUserOpt.isPresent()) {
            User dbUser = dbUserOpt.get();
            dbUser.setEmail(email);
            userService.updateUser(dbUser);
        }

        emailService.sendEmailChangeEmail(email, currentUser.getFirstName());

        redirectAttributes.addFlashAttribute("successEmail", "Az email cím sikeresen frissítve!");
        return "redirect:/profile";
    }

    // ==========================================
    // 3. JELSZÓ MÓDOSÍTÁSA
    // ==========================================
    @PostMapping("/profile/update-password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        Optional<User> userOpt = getCurrentUser(auth);
        if (userOpt.isEmpty()) return "redirect:/login";
        User currentUser = userOpt.get();

        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorPassword", "A jelenlegi jelszó helytelen!");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Az új jelszavak nem egyeznek!");
            return "redirect:/profile";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorPassword", "Túl rövid jelszó!");
            return "redirect:/profile";
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(currentUser);

        emailService.sendPasswordChangeEmail(currentUser.getEmail(), currentUser.getFirstName());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("success", "A jelszavad megváltozott. Kérlek, jelentkezz be az új jelszavaddal!");
        return "redirect:/login";
    }

    // ==========================================
    // EGYÉB OLDALAK (Kurzusaim, Törlés, Rólunk)
    // ==========================================

    @GetMapping("/my-courses")
    public String myCourses(Model model, Authentication auth) {
        Optional<User> userOpt = getCurrentUser(auth);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("enrolledCourses", courseService.getEnrolledCourses(user));
            model.addAttribute("teachingCourses", courseService.getCoursesByInstructor(user));
        }
        return "my-courses";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(Authentication auth,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {

        Optional<User> userOpt = getCurrentUser(auth);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();

        emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName());

        userService.deleteUser(user.getId());

        try {
            request.logout();
        } catch (Exception e) {
            System.err.println("Logout hiba törléskor: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("success",
                "Fiókod sikeresen törölve lett. Viszontlátásra!");
        return "redirect:/login";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}