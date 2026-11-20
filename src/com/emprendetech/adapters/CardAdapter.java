package com.emprendetech.adapters;

import com.emprendetech.business.payments.PaymentGateway;

public class CardAdapter implements PaymentGateway {
    @Override
    public boolean processPayment(double amount, String buyerReference) {
        System.out.println("   [CardAdapter - HTTPS] Procesando con Red de Tarjetas (Sin guardar datos sensibles - RNF6)...");
        System.out.printf("   [CardAdapter] Transacción aprobada por $%.0f (Ref: %s)%n", amount, buyerReference);
        return true;
    }
}