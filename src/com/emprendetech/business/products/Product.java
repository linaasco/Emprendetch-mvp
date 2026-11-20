package com.emprendetech.business.products;

public class Product {
    private Long id;
    private String name;
    private String category;
    private String locality;
    private double price;
    private int stock;

    public Product(Long id, String name, String category, String locality, double price, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.locality = locality;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocality() { return locality; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public void reduceStock(int quantity) { this.stock -= quantity; }

    @Override
    public String toString() {
        return String.format("[%d] %s | Localidad: %s | Categoría: %s | Precio: $%.0f | Stock: %d",
                id, name, locality, category, price, stock);
    }
}