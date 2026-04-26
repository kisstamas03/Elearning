package com.melearning.elearning.controller;

import com.melearning.elearning.model.*;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.LessonService;
import com.melearning.elearning.service.PresentationService;
import com.melearning.elearning.service.QuizService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private PresentationService presentationService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private LessonService lessonService;

    // -----------------------------------------------------------------------
    // Segédmetódusok
    // -----------------------------------------------------------------------

    private Optional<User> getCurrentUser(Authentication auth) {
        if (auth == null) return Optional.empty();
        return userService.getUserByUsername(auth.getName());
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean isOwnerOrAdmin(Course course, User user) {
        return isAdmin(user) || course.getInstructor().getId().equals(user.getId());
    }

    // -----------------------------------------------------------------------
    // Kurzus lista (Keresővel kiegészítve)
    // -----------------------------------------------------------------------

    @GetMapping
    public String listCourses(Model model, Authentication auth,
                              @RequestParam(value = "keyword", required = false) String keyword) {
        List<Course> courses;
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            if (isAdmin(currentUser)) {
                courses = courseService.getAllCourses();
            } else if (currentUser.getRole() == Role.INSTRUCTOR) {
                courses = courseService.getCoursesByInstructor(currentUser);
            } else {
                List<Course> publicCourses  = courseService.getPublicCourses();
                List<Course> enrolledCourses = courseService.getEnrolledCourses(currentUser);
                courses = new ArrayList<>(publicCourses);
                for (Course enrolledCourse : enrolledCourses) {
                    if (!courses.contains(enrolledCourse)) courses.add(enrolledCourse);
                }
            }
        } else {
            courses = courseService.getPublicCourses();
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.toLowerCase().trim();
            courses = courses.stream()
                    .filter(c -> c.getTitle().toLowerCase().contains(searchKeyword) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(searchKeyword)))
                    .toList();
        }

        model.addAttribute("courses", courses);
        model.addAttribute("keyword", keyword);

        return "courses/list";
    }

    // -----------------------------------------------------------------------
    // Kurzus megtekintése
    // -----------------------------------------------------------------------

    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        if (courseOpt.isEmpty()) return "redirect:/courses";

        Course courseObj = courseOpt.get();
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();

            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            boolean isInstructorOfThisCourse = (currentUser.getRole() == Role.INSTRUCTOR
                    && courseObj.getInstructor() != null
                    && courseObj.getInstructor().getId().equals(currentUser.getId()));

            if (isAdmin || isInstructorOfThisCourse) {
                return "redirect:/courses/" + id + "/manage";
            }
        }

        if (!courseObj.getIsPublic()) {
            if (userOpt.isEmpty()) return "redirect:/courses";
            User currentUser = userOpt.get();

            boolean isEnrolled = courseObj.getEnrolledUsers().contains(currentUser);
            if (!isEnrolled) {
                return "redirect:/courses";
            }
        }

        model.addAttribute("course", courseObj);
        model.addAttribute("presentations", presentationService.getPresentationsByCourse(courseObj));
        model.addAttribute("quizzes", quizService.getQuizzesByCourse(courseObj));
        model.addAttribute("lessons", lessonService.findByCourseId(courseObj.getId()));

        userOpt.ifPresent(user -> {
            model.addAttribute("isEnrolled", courseObj.getEnrolledUsers().contains(user));
            model.addAttribute("user", user);
        });

        if (!model.containsAttribute("isEnrolled")) {
            model.addAttribute("isEnrolled", false);
        }

        return "courses/view";
    }

// -----------------------------------------------------------------------
// Kurzus létrehozása
// -----------------------------------------------------------------------

    @GetMapping("/create")
    public String createCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@Valid @ModelAttribute("course") Course course,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {

        Optional<User> instructorOpt = getCurrentUser(auth);
        if (instructorOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Oktató nem található!");
            return "redirect:/courses/create";
        }

        User instructor = instructorOpt.get();
        if (instructor.getRole() != Role.INSTRUCTOR && !isAdmin(instructor)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod kurzus létrehozásához!");
            return "redirect:/courses";
        }

        try {
            course.setInstructor(instructor);
            courseService.saveCourse(course);

            redirectAttributes.addFlashAttribute("success", "Kurzus sikeresen létrehozva!");
            return "redirect:/courses";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Hiba történt a kurzus mentése során: " + e.getMessage());
            return "redirect:/courses/create";
        }
    }

    // -----------------------------------------------------------------------
    // Kurzus törlése (oktató vagy admin)
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/delete")
    public String deleteCourse(@PathVariable Long id,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> userOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || userOpt.isEmpty()) return "redirect:/courses";

        Course course = courseOpt.get();
        User user = userOpt.get();

        if (!isOwnerOrAdmin(course, user)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez a művelethez!");
            return "redirect:/courses/" + id + "/manage";
        }

        courseService.deleteCourse(id);
        redirectAttributes.addFlashAttribute("success", "Kurzus sikeresen törölve!");
        return "redirect:/courses";
    }

    // -----------------------------------------------------------------------
    // Prezentáció hozzáadása meglévő kurzushoz (Duplikáció szűréssel)
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/add-presentations")
    public String addPresentations(@PathVariable Long id,
                                   @RequestParam("newPresentations") MultipartFile[] files,
                                   @RequestParam(required = false) Long lessonId,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> userOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || userOpt.isEmpty()) return "redirect:/courses";

        Course course = courseOpt.get();
        User user = userOpt.get();

        if (!isOwnerOrAdmin(course, user)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez!");
            return "redirect:/courses/" + id;
        }

        Lesson selectedLesson = null;
        if (lessonId != null) {
            selectedLesson = lessonService.findById(lessonId).orElse(null);
        }

        try {
            List<Presentation> existingPres = presentationService.getPresentationsByCourse(course);
            List<String> existingNames = existingPres.stream().map(Presentation::getFileName).toList();

            int order = existingPres.size() + 1;
            int addedCount = 0;
            int skippedCount = 0;

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalName = file.getOriginalFilename();
                    if (originalName != null) {
                        String lowerName = originalName.toLowerCase();

                        if (lowerName.endsWith(".pdf") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {

                            String targetFileName = originalName;
                            if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
                                targetFileName = originalName.substring(0, originalName.lastIndexOf('.')) + ".pdf";
                            }

                            if (existingNames.contains(targetFileName)) {
                                skippedCount++;
                                continue;
                            }

                            presentationService.processAndSavePresentation(file, course, selectedLesson, order++);
                            addedCount++;
                        }
                    }
                }
            }

            if (skippedCount > 0 && addedCount > 0) {
                redirectAttributes.addFlashAttribute("success", addedCount + " fájl sikeresen feltöltve. " + skippedCount + " fájl kihagyva, mert már létezik.");
            } else if (skippedCount > 0 && addedCount == 0) {
                redirectAttributes.addFlashAttribute("error", "Minden kiválasztott fájl már létezik a kurzusban!");
            } else if (addedCount > 0) {
                redirectAttributes.addFlashAttribute("success", "Prezentációk sikeresen feltöltve!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Nem sikerült egyetlen érvényes fájlt sem feltölteni.");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hiba a feltöltés során: " + e.getMessage());
        }

        return "redirect:/courses/" + id + "/manage";
    }

    // -----------------------------------------------------------------------
    // Prezentáció törlése
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/delete-presentation")
    public String deletePresentation(@PathVariable Long id,
                                     @RequestParam Long presentationId,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> userOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || userOpt.isEmpty()) return "redirect:/courses";

        Course course = courseOpt.get();
        User user = userOpt.get();

        if (!isOwnerOrAdmin(course, user)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez!");
            return "redirect:/courses/" + id + "/manage";
        }

        presentationService.deletePresentation(presentationId);
        redirectAttributes.addFlashAttribute("success", "Prezentáció sikeresen törölve!");
        return "redirect:/courses/" + id + "/manage";
    }

    // -----------------------------------------------------------------------
    // Beiratkozás
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/enroll")
    public String enrollInCourse(@PathVariable Long id, Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> userOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || userOpt.isEmpty()) return "redirect:/courses";

        Course course = courseOpt.get();
        User user = userOpt.get();

        if (isOwnerOrAdmin(course, user)) {
            redirectAttributes.addFlashAttribute("error", "Oktatóként nem iratkozhatsz be saját kurzusodra!");
            return "redirect:/courses/" + id;
        }

        if (!course.getIsPublic()) {
            redirectAttributes.addFlashAttribute("error",
                    "Ez egy privát kurzus, csak az oktató adhat hozzá diákokat!");
            return "redirect:/courses/" + id;
        }

        courseService.enrollUser(course, user);
        return "redirect:/courses/" + id;
    }

    // -----------------------------------------------------------------------
    // Kurzus kezelése
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/manage")
    public String manageCourse(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        if (courseOpt.isEmpty()) return "redirect:/courses";

        Course courseObj = courseOpt.get();
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isEmpty() || !isOwnerOrAdmin(courseObj, userOpt.get())) {
            return "redirect:/courses/" + id;
        }

        model.addAttribute("course", courseObj);
        model.addAttribute("quizzes", quizService.getQuizzesByCourse(courseObj));
        model.addAttribute("presentations", presentationService.getPresentationsByCourse(courseObj));
        model.addAttribute("lessons", lessonService.findByCourseId(courseObj.getId()));

        model.addAttribute("allStudents", userService.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .toList());

        return "courses/manage";
    }

    @PostMapping("/{id}/add-student")
    public String addStudent(@PathVariable Long id,
                             @RequestParam Long studentId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> studentOpt = userService.getUserById(studentId);
        Optional<User> currentUserOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || studentOpt.isEmpty() || currentUserOpt.isEmpty())
            return "redirect:/courses";

        Course course = courseOpt.get();
        User currentUser = currentUserOpt.get();

        if (!isOwnerOrAdmin(course, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez a művelethez!");
            return "redirect:/courses/" + id;
        }

        User student = studentOpt.get();
        if (student.getRole() != Role.STUDENT) {
            redirectAttributes.addFlashAttribute("error", "Csak hallgatókat lehet beiratkoztatni!");
            return "redirect:/courses/" + id + "/manage";
        }

        courseService.enrollUser(course, student);
        redirectAttributes.addFlashAttribute("success", "Diák sikeresen hozzáadva!");
        return "redirect:/courses/" + id + "/manage";
    }

    @PostMapping("/{id}/remove-student")
    public String removeStudent(@PathVariable Long id,
                                @RequestParam Long studentId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> studentOpt = userService.getUserById(studentId);
        Optional<User> currentUserOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || studentOpt.isEmpty() || currentUserOpt.isEmpty())
            return "redirect:/courses";

        Course course = courseOpt.get();
        User currentUser = currentUserOpt.get();

        if (!isOwnerOrAdmin(course, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez a művelethez!");
            return "redirect:/courses/" + id;
        }

        courseService.unenrollUser(course, studentOpt.get());
        redirectAttributes.addFlashAttribute("success", "Diák eltávolítva!");
        return "redirect:/courses/" + id + "/manage";
    }
}