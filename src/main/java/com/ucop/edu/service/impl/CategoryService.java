package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Categories;
import com.ucop.edu.repository.CategoryRepository;

import java.util.List;

public class CategoryService {
    private final CategoryRepository repo = new CategoryRepository();

    public List<Categories> findAll() {
        return repo.findAll();
    }
}
