package com.melearning.elearning.controller;

import com.melearning.elearning.model.Presentation;
import com.melearning.elearning.service.PresentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class PresentationController {

    @Autowired
    private PresentationService presentationService;

    @GetMapping("/presentations/{id}/view")
    public String viewPresentation(@PathVariable Long id, Model model) {
        Optional<Presentation> presentationOpt = presentationService.getPresentationById(id);

        if (presentationOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Presentation presentation = presentationOpt.get();
        model.addAttribute("presentation", presentation);
        model.addAttribute("courseId", presentation.getCourse().getId());

        return "presentation/viewer";
    }

}