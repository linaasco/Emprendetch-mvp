package com.emprendetech;

import com.emprendetech.api.EmprendeTechController;
import com.emprendetech.business.checkout.CheckoutFacade;
import com.emprendetech.business.products.ProductSearchService;
import com.emprendetech.data.ProductRepository;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Inicialización de Capas del Monolito Modular
        ProductRepository repository = new ProductRepository();
        ProductSearchService searchService = new ProductSearchService(repository);
        CheckoutFacade checkoutFacade = new CheckoutFacade(searchService);
        EmprendeTechController apiController = new EmprendeTechController(searchService, checkoutFacade);

        // 2. Encender el servidor REST en el puerto 8080
        apiController.startServer(8080);
    }
}