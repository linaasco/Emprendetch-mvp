# EmprendeTech 1.0 — Producto Mínimo Viable (PMV)

**Plataforma E-Commerce para Emprendedores Locales de Bogotá (Cámara de Comercio de Bogotá)**  
**Actividad:** Fase 2 — Actividad A2.3 (Implementación de PMV y Documentación Arquitectónica)  
**Lenguaje y Tecnología:** Java 17+ (Estándar nativo `com.sun.net.httpserver.HttpServer`, sin dependencias externas)

---

## 1. Propósito del PMV y Alcance Funcional

Este repositorio contiene el **Producto Mínimo Viable (PMV)** del backend de **EmprendeTech**, diseñado para evidenciar en código ejecutable la arquitectura y los patrones de diseño documentados en el *Software Architecture Document (SAD)*.

### Requerimientos Funcionales Implementados (Superando el mínimo de 2 exigidos)
1. **RF3 — Búsqueda y filtrado de productos:** Consulta de catálogo filtrando simultáneamente por **localidad de Bogotá** (ej. Chapinero, Usaquén, Suba), **categoría** y **precio máximo**, garantizando tiempos de respuesta inferiores a 2 segundos (**RNF3**).
2. **RF4 — Carrito de compras y proceso de checkout:** Orquestación de compra que valida la existencia del producto, verifica disponibilidad de inventario, calcula el monto total y descuenta el stock de manera atómica tras confirmar el pago.
3. **RF5 — Integración con pasarela de pagos múltiple:** Soporte extensible para pagos mediante **Nequi**, **Daviplata**, **Tarjeta de Crédito/Débito** y **Efectivo contraentrega**, protegiendo los datos financieros sensibles mediante tokenización (**RNF6**).

---

## 2. Decisiones Arquitectónicas y Análisis de Trade-Offs

En arquitectura de software no existen soluciones perfectas, sino **compromisos (trade-offs)** donde se priorizan ciertos atributos de calidad (RNF) asumiendo riesgos controlados sobre otros. A continuación se justifican las decisiones tomadas tanto a nivel de estilo arquitectónico como de patrones de diseño:

### 2.1 Estilo Arquitectónico: Monolito Modular en N-Capas
* **Decisión:** Estructurar el sistema como una única unidad desplegable dividida lógicamente en paquetes que representan capas y módulos cohesivos (`api` $\rightarrow$ `business` $\rightarrow$ `adapters` y `data`), comunicándose mediante llamadas en memoria (*in-process*).
* **¿Qué ganamos? (Beneficios):**
  * **Bajo costo inicial (RNF4):** Se despliega un solo artefacto ejecutándose en un único proceso, evitando los altos costos de infraestructura, orquestadores (Kubernetes) y múltiples bases de datos que exigirían los Microservicios.
  * **Baja latencia / Rendimiento (RNF3 < 2 s):** La comunicación entre el controlador REST, el servicio de búsqueda, el checkout y los adaptadores ocurre directamente en memoria RAM sin serialización de red interna.
* **¿Qué arriesgamos / perdimos? (Trade-off):**
  * **Escalamiento granular independiente (RNF2 y RNF7):** Perdimos la capacidad de escalar únicamente el módulo de pagos o de búsqueda por separado; si crece la demanda, debe replicarse toda la aplicación completa.
  * **Aislamiento de fallos (RNF1):** Un error crítico de memoria en un módulo podría afectar el proceso completo. *Mitigación en el SAD:* Se diseñó el backend sin estado (*stateless*) para replicarlo horizontalmente detrás de un Balanceador de Carga.

### 2.2 Trade-Offs de los Patrones de Diseño Seleccionados

#### A. Patrón `Facade` (Estructural) — Clase `CheckoutFacade`
* **¿Por qué lo usamos?** El proceso de compra requiere coordinar tres subprocesos: consultar el producto/inventario (`ProductSearchService`), seleccionar/ejecutar el cobro (`PaymentGatewayFactory` + `PaymentGateway`) y actualizar el stock. `CheckoutFacade` expone un único método sencillo (`executeCheckout(...)`) hacia la capa API.
* **Alternativas descartadas y Trade-off:**
  * *Alternativa 1 (Orquestación directa en el Controlador API):* Si hubiéramos puesto la validación de stock y el cobro dentro de `EmprendeTechController`, habríamos violado la separación de capas (acoplando HTTP con reglas de negocio).
  * *Alternativa 2 (Patrón Mediator):* Aunque `Mediator` permite comunicación bidireccional compleja entre colegas, aquí el flujo es unidireccional y jerárquico (de la API hacia los subsistemas). Usar `Mediator` habría añadido complejidad innecesaria.
  * *¿Qué arriesgamos con Facade?* Si el sistema crece mucho, una fachada puede convertirse en un *"God Object"* (una clase que sabe demasiado y centraliza demasiada lógica). Lo mitigamos delegando la creación de pasarelas al `Factory` y el cobro al `Strategy`.

#### B. Patrones `Strategy` (Comportamiento) + `Adapter` (Estructural) — `PaymentGateway` y paquete `adapters`
* **¿Por qué los usamos juntos?**
  * `Strategy` (`PaymentGateway`) permite intercambiar dinámicamente el algoritmo de pago en tiempo de ejecución según lo que elija el comprador, eliminando bloques gigantes de `if/else` y manteniendo **baja complejidad ciclomática (RNF5)**.
  * `Adapter` (`NequiAdapter`, `DaviplataAdapter`, `CardAdapter`, `CashAdapter`) traduce nuestra interfaz interna estándar hacia los protocolos y formatos específicos de cada entidad financiera externa, aislando los detalles de encriptación y tokens (**RNF6**).
* **Alternativas descartadas y Trade-off:**
  * *Alternativa (Condicionales `if/else` o `switch` dentro del Checkout):* Habría sido más rápido de escribir inicialmente, pero violaría el **Principio Abierto/Cerrado (OCP)**: cada vez que la Cámara de Comercio pidiera agregar PSE o Apple Pay, tocaría modificar y volver a probar la clase crítica de `Checkout`, disparando la complejidad ciclomática.
  * *¿Qué perdimos / arriesgamos?* **Proliferación de clases.** En lugar de tener un solo archivo con `if/else`, ahora tenemos una interfaz y 4 clases separadas (una por cada medio de pago), aumentando ligeramente la cantidad de archivos en el proyecto a cambio de extensibilidad limpia.

#### C. Patrón `Factory Method` (Creacional) — Clase `PaymentGatewayFactory`
* **¿Por qué lo usamos?** Centraliza la lógica de instanciación (`new NequiAdapter()`, `new DaviplataAdapter()`, etc.) a partir de un texto recibido en el JSON (`"NEQUI"`, `"DAVIPLATA"`, `"CARD"`, `"CASH"`).
* **Alternativas descartadas y Trade-off:**
  * *Alternativa 1 (Instanciar con `new` dentro de `CheckoutFacade`):* Habría acoplado la lógica de negocio con las clases concretas de infraestructura.
  * *Alternativa 2 (Abstract Factory):* Se descartó porque no necesitamos crear familias de objetos relacionados (ej. pago + factura + reembolso por cada banco), sino un único objeto procesador de pago. `Abstract Factory` habría sido sobre-ingeniería (*over-engineering*).
  * *¿Qué perdimos?* El `switch` sigue existiendo en un único lugar aislado (`PaymentGatewayFactory`), lo que significa que al agregar una pasarela nueva sí debemos registrar su `case` en la fábrica (aunque sin tocar jamás la lógica de negocio del checkout).

#### D. Decisión Tecnológica del Servidor HTTP (`com.sun.net.httpserver.HttpServer`)
* **¿Por qué lo usamos?** Para exponer una **API REST real en JSON sobre HTTP** (consumible desde Postman, alineada con el Diagrama de Contenedores) usando exclusivamente el JDK estándar de Java.
* **Trade-off:** Sacrificamos las comodidades automáticas de frameworks pesados como *Spring Boot* (inyección de dependencias automática o serialización JSON con Jackson) a cambio de **cero dependencias externas, arranque instantáneo en milisegundos y total transparencia** sobre cómo viaja una petición HTTP desde la capa API hasta la capa de datos.

---

## 3. Estructura de Carpetas y Trazabilidad con el SAD

```text
Emprendetch-mvp/
├── README.md                                     <-- Documentación arquitectónica y decisiones
└── src/
    └── com/
        └── emprendetech/
            ├── Main.java                         <-- Ensamblaje de capas (Wiring) y arranque del servidor
            ├── api/                              <-- CAPA API (Presentation / Control Layer)
            │   └── EmprendeTechController.java
            ├── business/                         <-- CAPA DE NEGOCIO (Business Layer - Modules)
            │   ├── products/
            │   │   ├── Product.java
            │   │   └── ProductSearchService.java <-- RF3: Búsqueda y filtrado por localidad
            │   ├── payments/
            │   │   ├── PaymentGateway.java       <-- PATRÓN STRATEGY (Interfaz de dominio)
            │   │   └── PaymentGatewayFactory.java<-- PATRÓN FACTORY METHOD (Creacional)
            │   └── checkout/
            │       └── CheckoutFacade.java       <-- PATRÓN FACADE (RF4 y RF5)
            ├── adapters/                         <-- COMPONENTE ADAPTADORES DE PAGO (RF5)
            │   ├── NequiAdapter.java             <-- PATRÓN ADAPTER + STRATEGY
            │   ├── DaviplataAdapter.java
            │   ├── CardAdapter.java
            │   └── CashAdapter.java
            └── data/                             <-- CAPA DE ACCESO A DATOS (Data Access Layer)
                └── ProductRepository.java
```

---

## 4. Explicación Detallada de Clases y Métodos Esenciales

### 4.1 Punto de Entrada (`com.emprendetech`)
* **`Main.java` — Método `main(String[] args)`:**
  * **¿Por qué está ahí?** Actúa como el *Composition Root* (raíz de ensamblaje) del Monolito Modular. Instancia en orden estricto las dependencias de abajo hacia arriba (`ProductRepository` $\rightarrow$ `ProductSearchService` $\rightarrow$ `CheckoutFacade` $\rightarrow$ `EmprendeTechController`) mediante **inyección de dependencias manual por constructor**, y enciende el servidor HTTP en el puerto `8080`. Esto garantiza que ninguna capa dependa de variables globales estáticas.

### 4.2 Capa de Presentación / API (`com.emprendetech.api`)
* **`EmprendeTechController.java`:**
  * **`startServer(int port)`:** Crea el servidor HTTP nativo y mapea las rutas REST `/api/products` y `/api/checkout` definidas en el Diagrama de Componentes.
  * **`handleGetProducts(HttpExchange exchange)`:** Valida que el verbo HTTP sea `GET` (retornando `405 Method Not Allowed` si no lo es), extrae los parámetros de consulta (`locality`, `category`, `maxPrice`), delega la búsqueda a `ProductSearchService` y serializa la lista de objetos `Product` a una respuesta JSON con código `200 OK`.
  * **`handlePostCheckout(HttpExchange exchange)`:** Valida que el verbo sea `POST`, lee el flujo de bytes del cuerpo (`RequestBody`) en UTF-8, extrae los campos del JSON (`productId`, `quantity`, `paymentMethod`, `buyerRef`) y llama únicamente al método simplificado de `CheckoutFacade`. Si hay error de stock o pago, traduce la respuesta a HTTP `400 Bad Request`; si es exitoso, responde `200 OK`.

### 4.3 Capa de Negocio (`com.emprendetech.business`)
* **`products/Product.java`:**
  * Entidad de dominio que encapsula los atributos del producto (`id`, `name`, `category`, `locality`, `price`, `stock`).
  * **Método `reduceStock(int quantity)`:** Encapsula la mutación del inventario dentro de la propia entidad de dominio en lugar de permitir que clases externas modifiquen el atributo directamente.
* **`products/ProductSearchService.java`:**
  * **Método `searchProducts(String locality, String category, double maxPrice)`:** Implementa el caso de uso **RF3**, actuando como intermediario limpio entre el controlador API y el repositorio de datos.
* **`payments/PaymentGateway.java` (Interfaz — Patrón `Strategy`):**
  * **Método `boolean processPayment(double amount, String buyerReference)`:** Define el contrato único y polimórfico que deben cumplir todas las pasarelas. Gracias a esta firma, el módulo de checkout no sabe ni le importa si el dinero se cobra por celular (Nequi), tarjeta bancaria o en efectivo.
* **`payments/PaymentGatewayFactory.java` (Patrón `Factory Method`):**
  * **Método `getPaymentGateway(String paymentType)`:** Usa una expresión `switch` moderna de Java para instanciar y retornar la implementación concreta de `PaymentGateway`. Si recibe un método no soportado, lanza una excepción controlada `IllegalArgumentException` que el controlador captura y convierte en un mensaje claro para el cliente.
* **`checkout/CheckoutFacade.java` (Patrón `Facade`):**
  * **Método `executeCheckout(Long productId, int quantity, String paymentMethod, String buyerReference)`:** Es el núcleo de los requerimientos **RF4 y RF5**. Ejecuta 4 pasos en orden estricto:
    1. Consulta el producto y verifica `product == null || product.getStock() < quantity` (evitando ventas sin inventario).
    2. Calcula el valor total (`product.getPrice() * quantity`).
    3. Solicita el adaptador adecuado a `PaymentGatewayFactory` e invoca `gateway.processPayment(...)`.
    4. **Solo si `paymentSuccess` es `true`**, ejecuta `product.reduceStock(quantity)`, garantizando consistencia transaccional.

### 4.4 Componente de Adaptadores de Pago (`com.emprendetech.adapters`)
* **`NequiAdapter.java`, `DaviplataAdapter.java`, `CardAdapter.java`, `CashAdapter.java`:**
  * Cada clase implementa `PaymentGateway` (**Patrón `Adapter` + `Strategy`**).
  * **¿Por qué separar 4 clases?** En producción, Nequi usa notificaciones push al celular, Daviplata usa tokens OTP, las tarjetas usan pasarelas PCI-DSS y el efectivo solo genera un recibo contraentrega. Mantenerlos aislados en el paquete `adapters` permite que cada uno evolucione o cambie su protocolo HTTPS sin afectar al resto del sistema (**RNF5** y **RNF6**).

### 4.5 Capa de Acceso a Datos (`com.emprendetech.data`)
* **`ProductRepository.java`:**
  * **Constructor `ProductRepository()`:** Carga datos semilla en memoria (*Chapinero*, *Usaquén*, *Suba*) simulando las tablas relacionales de EmprendeTech.
  * **Método `findByFilters(String locality, String category, double maxPrice)`:** Utiliza *Java Streams* (`.stream().filter(...)`) para aplicar filtros dinámicos opcionales (si `locality` o `category` llegan nulos en la URL, ignora ese filtro sin lanzar `NullPointerException`) y garantiza respuestas en milisegundos (**RNF3 < 2 segundos**).

---

## 5. Guía de Ejecución y Pruebas en Postman

### 5.1 Compilación y Arranque del Servidor
Requisitos: **Java JDK 17** o superior instalado.

1. Abrir una terminal dentro de la carpeta `src`:
   ```bash
   cd src
   ```
2. Compilar el proyecto:
   ```bash
   javac com/emprendetech/Main.java
   ```
3. Iniciar el servidor REST en el puerto `8080`:
   ```bash
   java com.emprendetech.Main
   ```

### 5.2 Endpoints para Probar en Postman

#### Prueba 1: Búsqueda y Filtrado por Localidad (RF3)
* **Método:** `GET`
* **URL:** `http://localhost:8080/api/products?locality=Chapinero&category=Alimentos&maxPrice=30000`
* **Resultado esperado (`200 OK`):** Retorna un arreglo JSON únicamente con los productos de *Chapinero*, categoría *Alimentos* y precio menor o igual a `$30.000`.

#### Prueba 2: Checkout y Pago con Patrones Strategy/Adapter/Factory/Facade (RF4 y RF5)
* **Método:** `POST`
* **URL:** `http://localhost:8080/api/checkout`
* **Body (`raw` -> `JSON`):**
  ```json
  {
      "productId": 1,
      "quantity": 2,
      "paymentMethod": "NEQUI",
      "buyerRef": "3001234567"
  }
  ```
* **Resultado esperado (`200 OK`):**
  ```json
  {
      "status": "SUCCESS",
      "message": "ORDEN CONFIRMADA (Estado: PAGADO) - Producto: Cafe Organico Especial | Medio: NEQUI"
  }
  ```
* **Demostración de extensibilidad (RNF5):** Cambiar `"paymentMethod"` en el JSON por `"DAVIPLATA"`, `"CARD"` o `"CASH"` y volver a enviar la petición para comprobar en consola cómo cambia dinámicamente el adaptador ejecutado sin alterar la lógica del checkout.