# Standardized Error Handling

The Inventory API uses a standardized error response format inspired by Naver's API conventions and professional business logic error codes.

## 1. Error Response Format
All errors are returned in the following JSON format:

```json
{
  "errorCode": "I001",
  "errorMessage": "Item Not Found",
  "errorDetails": [
    {
      "field": "id",
      "value": "999",
      "reason": "No item exists with this ID"
    }
  ]
}
```

- **errorCode**: A custom alphanumeric code (e.g., I001, S001).
- **errorMessage**: A high-level, English description of the error.
- **errorDetails**: (Optional) An array of specific field-level errors, especially useful for validation failures.

## 2. Business Error Codes (ErrorCode Enum)
Custom codes are managed in the `ErrorCode` enum:
- `S001`: Internal Server Error (500)
- `C001`: Invalid Input Value (400)
- `I001`: Item Not Found (404)
- `I002`: Item Already Exists (409)

## 3. Global Exception Handler
The `GlobalExceptionHandler` class intercepts exceptions and maps them to the standard `ErrorResponse`. It automatically handles:
- `@Valid` validation errors (`MethodArgumentNotValidException`).
- Resource not found errors (`IllegalArgumentException`).
- Unhandled generic exceptions.
