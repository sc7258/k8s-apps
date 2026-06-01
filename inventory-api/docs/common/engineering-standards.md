# Engineering Standards (TDD, DDD, Clean Architecture)

This document defines the core engineering principles for the Inventory API project. Adhering to these standards ensures maintainability, testability, and a clear domain model.

## 1. Clean Architecture & Layered Structure
We follow a layered architecture with clear separation of concerns and Dependency Inversion (DIP).

- **Domain Layer**: Contains pure Java entities and business logic. It must NOT depend on external frameworks or libraries (including JPA annotations if strict separation is required).
- **Application Layer**: Contains Service classes (Use Cases) that coordinate domain objects.
- **Interface/Adapter Layer**: Contains Controllers, API Delegates, and Repository implementations.
- **Infrastructure Layer**: Contains external concerns like Database configuration, K8s manifests, and Telepresence setup.

## 2. Domain-Driven Design (DDD)
We strive for a **Rich Domain Model** over an Anemic one.

- **Encapsulation**: Avoid using `@Setter`. State changes should be performed through meaningful methods (e.g., `item.assignTo(user)` instead of `item.setStatus("ASSIGNED")`).
- **Validation**: Domain objects should be responsible for their own internal consistency and business rule validation.
- **Service vs. Entity**: Use Application Services only for coordination; business logic belongs in Entities or Domain Services.

## 3. Test-Driven Development (TDD)
Quality is guaranteed through a "Test-First" culture.

- **Red-Green-Refactor**: Always start by writing a failing test case before implementing a feature or fixing a bug.
- **Integration Testing**: Use **Testcontainers (MariaDB)** to verify the entire stack in an environment identical to production.
- **Contract Verification**: Tests must verify that the implementation strictly follows the OpenAPI specification.

## 4. API-First Development
- The `openapi/inventory-api.yaml` is the single source of truth for the API contract.
- Use the **Delegate Pattern** to decouple generated API infrastructure from business logic.
- Always regenerate code (`./gradlew openApiGenerate`) after modifying the specification.

## 5. Development Tools
- **Lombok**: Use to reduce boilerplate, but prefer `@Value` or manual constructors for immutable objects.
- **Swagger UI**: Use the original YAML specification for documentation to ensure 100% accuracy.
