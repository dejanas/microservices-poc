# Microservices exercise

A small shop split into three Spring Boot services. Each one owns its own data and its own API. They find each other through Eureka and trust the same signed token.

The point is to keep user accounts, the product catalog, and orders apart, then put back only the calls that one service genuinely needs from another.

## What each service owns

**User service** (port 8083) registers people and logs them in. Passwords are stored as BCrypt hashes. A new account receives the role `USER`. On startup the service creates a demo administrator, `admin` / `admin12345`, when that username is not already there. Login checks the password and returns a JWT. The token carries the username, the user id, and the roles, and it is valid for one hour.

**Product service** (port 8082) stores products. A product has a name, a description, a category, and a stock count. Anyone who is logged in can read and search the catalog. Search matches name, description, and category, and any of those filters can be left out. Only an `ADMIN` can create, update, or delete a product.

**Order service** (port 8081) stores orders. An order belongs to the user in the token and contains one or more products with a quantity. Any logged-in user can place an order. Reading, changing, or deleting an order is allowed for the user who placed it and for an administrator. `GET /api/orders/me` returns the caller's orders. An administrator can also ask for a specific user with `GET /api/orders/users/{userId}`.

**Discovery server** (port 8761) is a Eureka registry. The three services register there and call each other by service name (`user-service`, `product-service`) instead of a fixed host and port. The registry itself uses HTTP basic authentication (`eureka` / `password`).

Each service has its own in-memory H2 database. Stopping a service drops its data.

## How they cooperate

The user service is the only place that checks a password and the only place that issues a token. Product and order services do not call it on every request. They check the token signature locally with the same secret (`jwt.secret`, overridable with the `JWT_SECRET` environment variable). If the signature or the expiry is wrong, the request is rejected before it reaches the controller.

The order service does call the product service, because stock lives there. Placing an order reserves stock with one conditional update:

```sql
UPDATE products
SET stock = stock - :quantity
WHERE id = :id AND stock >= :quantity
```

If two orders ask for the last items at the same time, one update changes a row and the other changes none, so the second order is rejected. If a later line in the same order cannot be reserved, or saving the order fails, the order service releases what it already took. Updating an order reserves or releases only the difference. Deleting an order puts the stock back.

## Running it

Java 21 is required. Point `JAVA_HOME` at a JDK 21 if the `java` on your path is older. From the repository root, start the discovery server first, then the three services, each in its own terminal:

```powershell
.\mvnw.cmd -pl discovery-server spring-boot:run
.\mvnw.cmd -pl user-service spring-boot:run
.\mvnw.cmd -pl product-service spring-boot:run
.\mvnw.cmd -pl order-service spring-boot:run
```

`.\mvnw.cmd test` runs the tests. Those tests turn Eureka off so they do not need a registry.

Send the token as `Authorization: Bearer <token>`.

```http
POST http://localhost:8083/api/users/register
{"username":"ana","password":"password123"}

POST http://localhost:8083/api/users/login
{"username":"admin","password":"admin12345"}

POST http://localhost:8082/api/products
{"name":"Notebook","description":"A5 lined","category":"Stationery","stock":12}

GET http://localhost:8082/api/products/search?name=note&category=stationery

POST http://localhost:8081/api/orders
{"items":[{"productId":1,"quantity":2}]}

GET http://localhost:8081/api/orders/me
```

Creating a product requires the admin token. Placing and reading an order requires a normal user's token. Another user who asks for that order receives 403.
