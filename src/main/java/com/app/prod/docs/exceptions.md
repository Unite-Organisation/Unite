# Exception Handling

This document describes how exceptions are structured and handled in this project, and how to add a new exception type.

## Overview

The project uses a centralized exception-handling approach built around three main pieces:

1. **`AppException`** – a base runtime exception that all custom (business) exceptions extend.
2. **`AppError` / `Code`** – a structured error payload attached to exceptions, so every error returned to the client has a machine-readable code and a human-readable message.
3. **`GlobalExceptionHandler`** – a Spring `@RestControllerAdvice` that catches exceptions thrown anywhere in the request-handling pipeline and converts them into a consistent JSON response (`ApiErrorResponse`).

This design keeps controllers and services free of manual error-response building: code simply throws a typed exception, and the handler takes care of turning it into the correct HTTP status and response body.

## Core Components

### `AppError`

```java
public record AppError(
        @JsonProperty("code") Code code,
        String message
) {
    public AppError(Code code) {
        this(code, code.getDefaultMessage());
    }

    public static AppError of(Code code) { ... }
    public static AppError of(Code code, String message) { ... }
}
```

An `AppError` pairs a `Code` enum value with a message. If no custom message is supplied, the `Code`'s default message is used.

### `Code`

An enum listing every possible error code in the system (e.g. `USER_NOT_FOUND`, `BAD_CREDENTIALS`, `EMPTY_FILE`). Each entry has:
- `value` – the machine-readable string sent to clients (e.g. `"user_not_found"`)
- `defaultMessage` – the default human-readable message (e.g. `"User not found"`)

### `AppException`

```java
public class AppException extends RuntimeException {
    List<AppError> appErrors;

    public AppException(String message) { ... }
    public AppException(String message, AppError... appError) { ... }
    public AppException(AppError... appError) { ... }
    public AppException(List<AppError> appErrors) { ... }

    public List<AppError> getAppErrors() { return appErrors; }
}
```

`AppException` is the base class for all business/application exceptions. It carries one or more `AppError` values so that a single thrown exception can report multiple error codes if needed.

### `ApiErrorResponse`

```java
@Builder
public record ApiErrorResponse(
        int status,
        List<AppError> errors,
        String path,
        Instant timestamp
) {
    public static ApiErrorResponse of(HttpStatus status, List<AppError> errors, String path) { ... }
    public static ApiErrorResponse of(HttpStatus status, AppError error, String path) { ... }
}
```

This is the JSON body actually returned to API clients when an error occurs. It contains the HTTP status code, the list of `AppError`s, the request path, and a timestamp.

## Where Exceptions Are Caught

All exception handling is centralized in `GlobalExceptionHandler`, annotated with `@RestControllerAdvice`. Spring automatically routes any exception thrown from a controller (or from code called by a controller, such as a service) to the matching `@ExceptionHandler` method in this class — there is no need to catch exceptions manually inside controllers.


## Request Flow

1. A controller/service throws an exception (either a custom `AppException` subtype or a framework exception).
2. Spring intercepts it via `GlobalExceptionHandler` before it reaches the client.
3. The matching `@ExceptionHandler` method logs the error and builds an `ApiErrorResponse`.
4. The client receives a JSON response like:

```json
{
  "status": 404,
  "errors": [
    { "code": "user_not_found", "message": "User not found" }
  ],
  "path": "/api/users/42",
  "timestamp": "2026-07-24T10:15:30Z"
}
```

## How to Add a New Exception

Follow these steps whenever you need a new type of application error.

### 1. Add a new `Code`

Open `Code.java` and add a new enum constant with a unique `value` (snake_case string sent to clients) and a `defaultMessage`:

```java
ORDER_NOT_FOUND("order_not_found", "Order not found"),
```

### 2. Decide which exception class to use

- If an existing exception class already matches the semantics (e.g. `EntityNotPresentException` for "not found" cases, `BadRequestException` for validation failures), **reuse it** — just throw it with your new `Code`:

  ```java
  throw new EntityNotPresentException(AppError.of(Code.ORDER_NOT_FOUND));
  ```

- If none of the existing types fit (i.e. you need a new HTTP status or a genuinely new category of error), create a new exception class that extends `AppException`:

  ```java
  public class ConflictException extends AppException {
      public ConflictException(AppError... errors) {
          super(errors);
      }
  }
  ```

### 3. Register a handler for the new exception class (only if you created one)

If you created a brand-new exception class in step 2, add a corresponding method in `GlobalExceptionHandler`:

```java
@ExceptionHandler(ConflictException.class)
public ResponseEntity<ApiErrorResponse> handleException(ConflictException e, HttpServletRequest request) {
    log.error("Conflict occurred. Exception content: {}", e.getMessage());
    ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.CONFLICT, e.getAppErrors(), request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
}
```

Pick the `HttpStatus` that best represents the failure and keep the logging level consistent with similar handlers (`log.info` for expected/benign cases such as bad credentials, `log.error` for unexpected or server-side problems).

If you reused an existing exception class in step 2, **no changes to `GlobalExceptionHandler` are needed** — it already knows how to handle that exception type.

### 4. Throw the exception where the error occurs

```java
Order order = orderRepository.findById(id)
        .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.ORDER_NOT_FOUND)));
```

You can also supply a custom message (overriding the `Code`'s default) or pass multiple `AppError`s:

```java
throw new BadRequestException(
    AppError.of(Code.INVALID_TIME_PERIOD, "End date must be after start date")
);
```
