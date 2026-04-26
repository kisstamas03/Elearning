package com.melearning.elearning.config;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Lesson;
import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.repository.CourseRepository;
import com.melearning.elearning.repository.LessonRepository;
import com.melearning.elearning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Ellenőrizzük, hogy már vannak-e felhasználók az adatbázisban
        if (userRepository.count() == 0) {
            initializeUsers();
        }
    }

    private void initializeUsers() {
        // Admin felhasználó létrehozása
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@elearning.hu");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Felhasználó");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // Test oktató létrehozása
        User instructor = new User();
        instructor.setUsername("instructor");
        instructor.setEmail("instructor@elearning.hu");
        instructor.setPassword(passwordEncoder.encode("instructor123"));
        instructor.setFirstName("Teszt");
        instructor.setLastName("Oktató");
        instructor.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor);

        // Test hallgató létrehozása
        User student = new User();
        student.setUsername("student");
        student.setEmail("student@elearning.hu");
        student.setPassword(passwordEncoder.encode("student123"));
        student.setFirstName("Teszt");
        student.setLastName("Hallgató");
        student.setRole(Role.STUDENT);
        userRepository.save(student);

        // További oktatók létrehozása
        User instructor2 = new User();
        instructor2.setUsername("kovacs.janos");
        instructor2.setEmail("kovacs.janos@elearning.hu");
        instructor2.setPassword(passwordEncoder.encode("password123"));
        instructor2.setFirstName("János");
        instructor2.setLastName("Kovács");
        instructor2.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor2);

        User instructor3 = new User();
        instructor3.setUsername("nagy.anna");
        instructor3.setEmail("nagy.anna@elearning.hu");
        instructor3.setPassword(passwordEncoder.encode("password123"));
        instructor3.setFirstName("Anna");
        instructor3.setLastName("Nagy");
        instructor3.setRole(Role.INSTRUCTOR);
        userRepository.save(instructor3);

        System.out.println("=== TESZT FELHASZNÁLÓK LÉTREHOZVA ===");
        System.out.println("Admin - Username: admin, Password: admin123");
        System.out.println("Oktató - Username: instructor, Password: instructor123");
        System.out.println("Hallgató - Username: student, Password: student123");
        System.out.println("Oktató 2 - Username: kovacs.janos, Password: password123");
        System.out.println("Oktató 3 - Username: nagy.anna, Password: password123");
        System.out.println("=====================================");
    }

    private void createLesson(Course course, String title, String content, int order) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setOrderIndex(order);
        lesson.setCourse(course);
        lessonRepository.save(lesson);
    }
}