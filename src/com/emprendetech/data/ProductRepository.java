package com.emprendetech.data;

import com.emprendetech.business.products.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductRepository {
    private final List<Product> database = new ArrayList<>();

    public ProductRepository() {
        // Datos semilla de emprendedores de Bogotá (RF2 / RF3)
        database.add(new Product(1L, "Cafe Organico Especial", "Alimentos", "Chapinero", 28000.0, 15));
        database.add(new Product(2L, "Mochila Tejida a Mano", "Artesanias", "UsaQuen", 85000.0, 5));
        database.add(new Product(3L, "Postre de Natas Casero", "Alimentos", "Chapinero", 12000.0, 20));
        database.add(new Product(4L, "Chaqueta de Cuero Bogotana", "Ropa", "Suba", 150000.0, 8));
    }

    public List<Product> findByFilters(String locality, String category, double maxPrice) {
        return database.stream()
                .filter(p -> locality == null || p.getLocality().equalsIgnoreCase(locality))
                .filter(p -> category == null || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> p.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    public Product findById(Long id) {
        return database.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}