package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.CategoryRequest;
import com.chirag.shopsphere.dto.CategoryResponse;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.entity.Category;
import com.chirag.shopsphere.exception.DuplicateResourceException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryResponse.fromEntity(category);
    }

    public PageResponse<CategoryResponse> listCategories(Pageable pageable) {
        Page<CategoryResponse> page = categoryRepository.findAll(pageable)
                .map(CategoryResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String newName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByName(newName)) {
            throw new DuplicateResourceException("Category", "name", newName);
        }

        category.setName(newName);
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updated = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updated);
    }

    @Transactional
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setActive(false);
        categoryRepository.save(category);
    }
}
