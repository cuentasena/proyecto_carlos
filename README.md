## Monolito Modular (Java) – Boilerplate

Este proyecto es un **Monolito Modular** (un solo despliegue) con **separación por modulos** y comunicación **desacoplada** por un **Event Bus interno**.

### Estructura (modulos)

- `src/main/java/com/proyectocarlos/store/modules/catalog` (Catalogo)
  - Controller / Service / Repository (in-memory)
- `src/main/java/com/proyectocarlos/store/modules/inventory` (Inventario)
  - Controller / Service / Repository (in-memory)
- `src/main/java/com/proyectocarlos/store/modules/orders` (Pedidos)
  - Controller / Service / Repository (in-memory)
- `src/main/java/com/proyectocarlos/store/shared`
  - `events`: EventBus interno
  - `contracts`: eventos compartidos entre módulos (contratos)
  - `http`: servidor HTTP simple

### Comunicacion por eventos (ejemplo)

- `orders` publica `OrderPlacedEvent`
- `inventory` escucha `OrderPlacedEvent` y responde con:
  - `StockReservedEvent` o `StockRejectedEvent`
- `orders` escucha esos eventos para confirmar/rechazar el pedido

### Requisitos

- Java 17+
- Maven

### Ejecutar

```bash
mvn -q package
mvn -q exec:java
```

Arranca en `http://localhost:8080`.

### Prueba rapida (PowerShell)

Crear producto (sirve como “ítem de cafetería” o “libro”):

```powershell
$p = Invoke-RestMethod -Method Post -Uri http://localhost:8080/catalog/products -ContentType application/json -Body '{"name":"Latte","brand":"Cafetería Central","price":12000}'
$p.id
```

Agregar stock:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/inventory/stock/add -ContentType application/json -Body ("{`"productId`":`"$($p.id)`",`"quantity`":10}")
```

Crear pedido (dispara evento y reserva inventario):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/orders -ContentType application/json -Body ("{`"items`":[{`"productId`":`"$($p.id)`",`"quantity`":2}]}")
```

Ver pedidos:

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/orders
```

