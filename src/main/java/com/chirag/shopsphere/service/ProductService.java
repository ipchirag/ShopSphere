package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.dto.ProductRequest;
import com.chirag.shopsphere.dto.ProductResponse;
import com.chirag.shopsphere.entity.Category;
import com.chirag.shopsphere.entity.Product;
import com.chirag.shopsphere.exception.DuplicateResourceException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.CategoryRepository;
import com.chirag.shopsphere.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku().trim().toUpperCase())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.fromEntity(product);
    }

    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return ProductResponse.fromEntity(product);
    }

    public PageResponse<ProductResponse> listActiveProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findByActiveTrue(pageable)
                .map(ProductResponse::fromEntity);
        return PageResponse.from(page);
    }

    public PageResponse<ProductResponse> searchProducts(String name, Pageable pageable) {
        Page<ProductResponse> page = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable)
                .map(ProductResponse::fromEntity);
        return PageResponse.from(page);
    }

    public PageResponse<ProductResponse> filterByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        Page<ProductResponse> page = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(ProductResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String newSku = request.getSku().trim().toUpperCase();
        if (!product.getSku().equalsIgnoreCase(newSku) && productRepository.existsBySku(newSku)) {
            throw new DuplicateResourceException("Product", "sku", newSku);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(newSku);
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);
    }
}
