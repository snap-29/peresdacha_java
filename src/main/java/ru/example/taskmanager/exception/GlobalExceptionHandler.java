package ru.example.taskmanager.exception;

import java.io.IOException;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private ResponseEntity<Map<String, Object>> response(int code, String error, String message) {
    return ResponseEntity.status(code)
        .body(Map.of("status", code, "error", error, "message", message));
  }

  @ExceptionHandler(TaskNotFoundException.class)
  public ResponseEntity<?> notFound(TaskNotFoundException e) {
    return response(404, "Task not found", e.getMessage());
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<?> duplicate(DuplicateEmailException e) {
    return response(409, "Conflict", e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflict(DataIntegrityViolationException e) {
    return response(409, "Conflict", "Конфликт данных или email уже зарегистрирован");
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<?> unauthorized(AuthenticationException e) {
    return response(401, "Unauthorized", "Неверные данные входа или refresh-токен");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> forbidden(AccessDeniedException e) {
    return response(403, "Forbidden", e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(
            f ->
                fields.putIfAbsent(
                    f.getField(), Objects.toString(f.getDefaultMessage(), "Invalid value")));
    return ResponseEntity.badRequest()
        .body(Map.of("status", 400, "error", "Validation failed", "fields", fields));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<?> badRequest(Exception e) {
    return response(400, "Bad request", "Проверьте JSON, enum, параметры пагинации и сортировки");
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> state(IllegalStateException e) {
    return response(500, "Operation failed", "Демонстрация отката транзакции");
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<?> demo(IOException e) {
    return response(500, "Transaction demo", e.getMessage());
  }
}
