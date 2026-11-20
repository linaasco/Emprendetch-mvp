package com.emprendetech.adapters;

import com.emprendetech.business.payments.PaymentGateway;

public class NequiAdapter implements PaymentGateway {
    @Override
    public boolean processPayment(double amount, String buyerReference) {
        System.out.println("   [NequiAdapter - HTTPS] Conectando con API de Nequi...");
        System.out.printf("   [NequiAdapter] Cobro aprobado por $%.0f al celular %s (Token encriptado RNF6)%n", amount, buyerReference);
        return true;
    }
}