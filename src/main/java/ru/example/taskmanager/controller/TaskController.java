package ru.example.taskmanager.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.example.taskmanager.dto.*;
import ru.example.taskmanager.exception.TaskNotFoundException;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.service.TaskService;

@RestController
@RequestMapping({"/tasks", "/api/tasks"})
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @GetMapping
  public Page<TaskResponse> getAll(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) Priority priority,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sort,
      @AuthenticationPrincipal User user) {
    return service.getAll(status, priority, page, size, sort, user);
  }

  @GetMapping("/{id}")
  public TaskResponse getById(@PathVariable Long id, @AuthenticationPrincipal User user) {
    return service.getById(id, user);
  }

  @GetMapping("/my")
  public List<TaskResponse> my(@AuthenticationPrincipal User user) {
    return service.my(user);
  }

  @GetMapping("/stats")
  public Map<String, Long> stats(@AuthenticationPrincipal User user) {
    return service.stats(user);
  }

  @GetMapping("/search")
  public List<TaskResponse> search(
      @RequestParam String keyword, @AuthenticationPrincipal User user) {
    return service.search(keyword, user);
  }

  @GetMapping("/today")
  public List<TaskResponse> today(@AuthenticationPrincipal User user) {
    return service.today(user);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<TaskResponse> create(
      @Valid @RequestBody TaskRequest request, @AuthenticationPrincipal User user) {
    TaskResponse task = service.create(request, user);
    return ResponseEntity.created(URI.create("/tasks/" + task.id())).body(task);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public TaskResponse update(
      @PathVariable Long id,
      @Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal User user) {
    return service
        .update(id, request, user)
        .orElseThrow(() -> new TaskNotFoundException("Задача " + id + " не найдена"));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public TaskResponse updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody StatusRequest request,
      @AuthenticationPrincipal User user) {
    return service.updateStatus(id, request.status(), user);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
    service.delete(id, user);
    return ResponseEntity.noContent().build();
  }
}
