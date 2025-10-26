package com.melearning.elearning.controller;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "courses/list";
    }

    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Course> course = courseService.getCourseById(id);
        if (course.isPresent()) {
            model.addAttribute("course", course.get());

            if (auth != null) {
                Optional<User> user = userService.getUserByUsername(auth.getName());
                if (user.isPresent()) {
                    boolean isEnrolled = course.get().getEnrolledUsers().contains(user.get());
                    model.addAttribute("isEnrolled", isEnrolled);
                    model.addAttribute("user", user.get());
                }
            }

            return "courses/view";
        }
        return "redirect:/courses";
    }

    @GetMapping("/create")
    public String createCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@ModelAttribute Course course, Authentication auth) {
        Optional<User> instructor = userService.getUserByUsername(auth.getName());
        if (instructor.isPresent()) {
            course.setInstructor(instructor.get());
            courseService.saveCourse(course);
        }
        return "redirect:/courses";
    }

    @PostMapping("/{id}/enroll")
    public String enrollInCourse(@PathVariable Long id, Authentication auth) {
        Optional<Course> course = courseService.getCourseById(id);
        Optional<User> user = userService.getUserByUsername(auth.getName());

        if (course.isPresent() && user.isPresent()) {
            courseService.enrollUser(course.get(), user.get());
        }

        return "redirect:/courses/" + id;
    }
}