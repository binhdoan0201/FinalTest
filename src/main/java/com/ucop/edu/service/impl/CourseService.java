package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Course;
import com.ucop.edu.repository.CourseRepository;

import java.util.List;

public class CourseService {
    private final CourseRepository repo = new CourseRepository();

    public List<Course> findAll() {
        return repo.findAll();
    }

    public void save(Course c) {
        repo.save(c);
    }

    public void update(Course c) {
        repo.update(c);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public List<Course> search(Long categoryId, String keyword) {
        return repo.search(categoryId, keyword);
    }
}
