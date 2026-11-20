package com.emprendetech.business.payments;

import com.emprendetech.adapters.*;

// PATRÓN FACTORY METHOD (Creacional): Desacopla la creación de adaptadores de pago
public class PaymentGatewayFactory {

    public static PaymentGateway getPaymentGateway(String paymentType) {
        return switch (paymentType.toUpperCase()) {
            case "NEQUI" -> new NequiAdapter();
            case "DAVIPLATA" -> new DaviplataAdapter();
            case "CARD" -> new CardAdapter();
            case "CASH" -> new CashAdapter();
            default -> throw new IllegalArgumentException("Método de pago no soportado: " + paymentType);
        };
    }
}