package com.emprendetech.business.products;

import com.emprendetech.data.ProductRepository;
import java.util.List;

public class ProductSearchService {
    private final ProductRepository productRepository;

    public ProductSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // RF3: Búsqueda rápida en memoria (< 2 segundos para cumplir RNF3)
    public List<Product> searchProducts(String locality, String category, double maxPrice) {
        return productRepository.findByFilters(locality, category, maxPrice);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id);
    }
}