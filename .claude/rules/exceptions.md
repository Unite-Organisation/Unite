## Exceptions

Every exception thrown by our own code must extend `AppException`
(`com.app.prod.exceptions.exceptions.AppException`) and must be handled in
`GlobalExceptionHandler` (`com.app.prod.exceptions.handler.GlobalExceptionHandler`).

Never throw raw `RuntimeException`, `IllegalStateException` or `IllegalArgumentException`
from services - use an `AppException` subtype so the API returns a consistent
`ApiErrorResponse` instead of falling through to the generic 500 handler.

### Adding a new exception

1. Add a `Code` entry in `com.app.prod.exceptions.Code` with a snake_case code and a default message
```java
BUILDING_NOT_FOUND("building_not_found", "Building not found"),
```

2. Reuse an existing exception if one fits. Current types and their HTTP statuses:

| Exception                          | Status |
|------------------------------------|--------|
| `BadRequestException`              | 400    |
| `EmptyFileException`               | 400    |
| `InvalidFileExtensionException`    | 400    |
| `UnauthorizedDataAccessException`  | 403    |
| `EntityNotPresentException`        | 404    |
| `DataAlreadyExistsException`       | 409    |
| `FileNotFoundException`            | 500    |
| `IllegalApplicationStateException` | 500    |
| `InternalConnectionException`      | 500    |

3. Only if none fits, create a new class in `com.app.prod.exceptions.exceptions` extending `AppException`
```java
public class PollAlreadyClosedException extends AppException {
    public PollAlreadyClosedException(AppError... errors) {
        super(errors);
    }
}
```

4. A new exception class always requires a matching handler in `GlobalExceptionHandler`,
   added in the `AppExceptions` section - one status code per exception type
```java
@ExceptionHandler(PollAlreadyClosedException.class)
public ResponseEntity<ApiErrorResponse> handleException(PollAlreadyClosedException e, HttpServletRequest request) {
    log.error("Poll is already closed. Exception content: {}", e.getMessage());
    ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.CONFLICT, e.getAppErrors(), request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
}
```

### Throwing

Always pass `AppError` - never only a plain message, because `getAppErrors()` is what
reaches the client. Use the `Code` default message, or override it when extra context helps.
```java
throw new EntityNotPresentException(AppError.of(Code.BUILDING_NOT_FOUND));
throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, String.format("%s is after %s", startTime, endTime)));
```

Throw from the service layer. Controllers do not catch `AppException` and do not build
error responses on their own - that is the job of `GlobalExceptionHandler`.

### Rules for the handler

- `@ExceptionHandler(Exception.class)` is the last-resort fallback returning `UNKNOWN_ERROR` with 500.
  Hitting it means an exception was missed - fix the exception type instead of leaving it there.
- Client-caused errors (4xx) are logged with `log.info` or `log.error`, server faults (5xx) with `log.error`.
- Never leak internal messages of unhandled exceptions into the response body.
