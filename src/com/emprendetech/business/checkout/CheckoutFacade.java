package com.emprendetech.business.checkout;

import com.emprendetech.business.payments.PaymentGateway;
import com.emprendetech.business.payments.PaymentGatewayFactory;
import com.emprendetech.business.products.Product;
import com.emprendetech.business.products.ProductSearchService;

// PATRÓN FACADE (Estructural): Simplifica el proceso de compra coordinando Productos, Pagos y Pedidos
public class CheckoutFacade {
    private final ProductSearchService productService;

    public CheckoutFacade(ProductSearchService productService) {
        this.productService = productService;
    }

    // RF4 (Carrito y Checkout) + RF5 (Pasarela de pagos múltiple)
    public String executeCheckout(Long productId, int quantity, String paymentMethod, String buyerReference) {
        Product product = productService.getProductById(productId);

        if (product == null || product.getStock() < quantity) {
            return "ERROR: Producto no disponible o stock insuficiente.";
        }

        double totalAmount = product.getPrice() * quantity;
        System.out.printf("-> Iniciando Checkout: %d x %s | Total: $%.0f%n", quantity, product.getName(), totalAmount);

        // Uso de Factory Method + Strategy + Adapter para procesar el pago
        PaymentGateway gateway = PaymentGatewayFactory.getPaymentGateway(paymentMethod);
        boolean paymentSuccess = gateway.processPayment(totalAmount, buyerReference);

        if (paymentSuccess) {
            product.reduceStock(quantity);
            return "ORDEN CONFIRMADA (Estado: PAGADO) - Producto: " + product.getName() + " | Medio: " + paymentMethod;
        }
        return "ERROR: El pago fue rechazado.";
    }
}