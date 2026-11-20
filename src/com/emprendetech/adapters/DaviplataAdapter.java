package com.emprendetech.adapters;

import com.emprendetech.business.payments.PaymentGateway;

public class DaviplataAdapter implements PaymentGateway {
    @Override
    public boolean processPayment(double amount, String buyerReference) {
        System.out.println("   [DaviplataAdapter - HTTPS] Conectando con API de Daviplata...");
        System.out.printf("   [DaviplataAdapter] Cobro aprobado por $%.0f a la cuenta %s%n", amount, buyerReference);
        return true;
    }
}