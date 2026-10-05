package ru.example.taskmanager.dto;

import java.time.LocalDateTime;
import ru.example.taskmanager.model.Task;

public record TaskResponse(
    Long id,
    String title,
    String description,
    String priority,
    String status,
    LocalDateTime createdAt,
    Long ownerId) {
  public static TaskResponse from(Task t) {
    return new TaskResponse(
        t.getId(),
        t.getTitle(),
        t.getDescription(),
        t.getPriority().name(),
        t.getStatus().name(),
        t.getCreatedAt(),
        t.getOwner() == null ? null : t.getOwner().getId());
  }
}
