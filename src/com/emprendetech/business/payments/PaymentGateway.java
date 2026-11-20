package com.emprendetech.business.payments;

// PATRÓN STRATEGY: Interfaz común para todas las pasarelas de pago (RF5 y RNF5)
public interface PaymentGateway {
    boolean processPayment(double amount, String buyerReference);
}