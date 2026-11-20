package com.emprendetech.adapters;

import com.emprendetech.business.payments.PaymentGateway;

public class CashAdapter implements PaymentGateway {
    @Override
    public boolean processPayment(double amount, String buyerReference) {
        System.out.printf("   [CashAdapter] Pago en efectivo contraentrega registrado por $%.0f para %s%n", amount, buyerReference);
        return true;
    }
}