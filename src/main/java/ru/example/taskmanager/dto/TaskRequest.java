package ru.example.taskmanager.dto;

import jakarta.validation.constraints.*;
import ru.example.taskmanager.model.*;

/** Mutable DTO: shared by JSON requests and Thymeleaf form binding. */
public class TaskRequest {
  @NotBlank(message = "Название обязательно")
  @Size(max = 255)
  private String title;

  @Size(max = 10000)
  private String description;

  @NotNull private Priority priority = Priority.MEDIUM;
  @NotNull private Status status = Status.NEW;

  public TaskRequest() {}

  public TaskRequest(String title, String description, Priority priority, Status status) {
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.status = status;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
