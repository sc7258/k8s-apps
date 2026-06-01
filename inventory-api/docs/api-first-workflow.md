# API-First Development Workflow

The Inventory API follows an **API-First** approach where the OpenAPI specification is the single source of truth for the API contract.

## 1. The OpenAPI Specification
The API is defined in `openapi/inventory-api.yaml`. Any changes to the API (endpoints, parameters, models) must be made in this file first.

## 2. Code Generation (OpenAPI Generator)
We use the `org.openapi.generator` Gradle plugin to generate:
- **Models**: DTOs for requests and responses.
- **API Interfaces**: Spring MVC interfaces defining the endpoints.
- **Controller Stubs**: Pre-configured controllers that delegate logic.

### Generate Command
```bash
./gradlew openApiGenerate
```
The generated code is located in `build/generated/openapi`.

## 3. Implementation (Delegate Pattern)
We use the **Delegate Pattern** to separate generated code from business logic.
- **Generated**: `ItemManagementApiController`
- **Manual Implementation**: `ItemManagementDelegateImpl` (implements `ItemManagementApiDelegate`)

When the API spec changes, regenerate the code and update your Delegate implementation to match the new interface.

## 4. API Documentation (Swagger UI)
Swagger UI is available at `http://localhost:8080/swagger-ui.html`. It is configured to serve the original `inventory-api.yaml` file to ensure the documentation is always accurate.
