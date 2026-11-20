package com.emprendetech.api;

import com.emprendetech.business.checkout.CheckoutFacade;
import com.emprendetech.business.products.Product;
import com.emprendetech.business.products.ProductSearchService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// CAPA API: Servidor REST HTTP real en puerto 8080 (Alineado con Diagrama de Contenedores)
public class EmprendeTechController {
    private final ProductSearchService searchService;
    private final CheckoutFacade checkoutFacade;

    public EmprendeTechController(ProductSearchService searchService, CheckoutFacade checkoutFacade) {
        this.searchService = searchService;
        this.checkoutFacade = checkoutFacade;
    }

    public void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoint 1: GET /api/products?locality=Chapinero&category=Alimentos&maxPrice=30000 (RF3)
        server.createContext("/api/products", this::handleGetProducts);

        // Endpoint 2: POST /api/checkout (RF4 y RF5)
        server.createContext("/api/checkout", this::handlePostCheckout);

        server.setExecutor(null);
        server.start();
        System.out.println("==================================================");
        System.out.println("   SERVIDOR REST EMPRENDETECH ACTIVO EN PUERTO " + port);
        System.out.println("   Listo para recibir peticiones desde Postman");
        System.out.println("==================================================");
    }

    private void handleGetProducts(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\": \"Metodo no permitido. Use GET.\"}");
            return;
        }

        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String locality = params.get("locality");
        String category = params.get("category");
        double maxPrice = params.containsKey("maxPrice") ? Double.parseDouble(params.get("maxPrice")) : Double.MAX_VALUE;

        List<Product> results = searchService.searchProducts(locality, category, maxPrice);

        // Convertir resultados a formato JSON
        String jsonItems = results.stream()
                .map(p -> String.format(
                        "{\"id\": %d, \"name\": \"%s\", \"category\": \"%s\", \"locality\": \"%s\", \"price\": %.0f, \"stock\": %d}",
                        p.getId(), p.getName(), p.getCategory(), p.getLocality(), p.getPrice(), p.getStock()))
                .collect(Collectors.joining(", "));

        sendJsonResponse(exchange, 200, "[" + jsonItems + "]");
    }

    private void handlePostCheckout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\": \"Metodo no permitido. Use POST.\"}");
            return;
        }

        // Leer el cuerpo JSON enviado desde Postman
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> jsonMap = parseSimpleJson(body);

        try {
            Long productId = Long.parseLong(jsonMap.get("productId"));
            int quantity = Integer.parseInt(jsonMap.get("quantity"));
            String paymentMethod = jsonMap.get("paymentMethod");
            String buyerRef = jsonMap.get("buyerRef");

            String result = checkoutFacade.executeCheckout(productId, quantity, paymentMethod, buyerRef);
            int statusCode = result.startsWith("ERROR") ? 400 : 200;

            String jsonResponse = String.format("{\"status\": \"%s\", \"message\": \"%s\"}",
                    statusCode == 200 ? "SUCCESS" : "FAILED", result);

            sendJsonResponse(exchange, statusCode, jsonResponse);
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, "{\"error\": \"JSON invalido. Envie productId, quantity, paymentMethod y buyerRef.\"}");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) map.put(pair[0], pair[1]);
        }
        return map;
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        String clean = json.replaceAll("[{}\"]", "");
        for (String entry : clean.split(",")) {
            String[] pair = entry.split(":");
            if (pair.length == 2) map.put(pair[0].trim(), pair[1].trim());
        }
        return map;
    }
}