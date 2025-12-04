package com.melearning.elearning.service;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Presentation;
import com.melearning.elearning.repository.PresentationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PresentationService {

    @Autowired
    private PresentationRepository presentationRepository;

    public List<Presentation> getPresentationsByCourse(Course course) {
        return presentationRepository.findByCourseOrderByOrderIndex(course);
    }

    public Optional<Presentation> getPresentationById(Long id) {
        return presentationRepository.findById(id);
    }

    public Presentation savePresentation(Presentation presentation) {
        return presentationRepository.save(presentation);
    }

    public void deletePresentation(Long id) {
        presentationRepository.deleteById(id);
    }
}