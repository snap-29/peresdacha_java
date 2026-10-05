package ru.example.taskmanager.service;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.taskmanager.dto.*;
import ru.example.taskmanager.exception.TaskNotFoundException;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.repository.TaskRepository;

@Service
@Transactional
public class TaskService {
  private final TaskRepository taskRepository;
  private final AuditService auditService;

  public TaskService(TaskRepository taskRepository, AuditService auditService) {
    this.taskRepository = taskRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getAll() {
    return taskRepository.findAll().stream().map(TaskResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public Optional<TaskResponse> findById(Long id) {
    return taskRepository.findById(id).map(TaskResponse::from);
  }

  @Transactional(readOnly = true)
  public TaskResponse getOrThrow(Long id) {
    return TaskResponse.from(entity(id));
  }

  @Transactional(readOnly = true)
  public TaskResponse getById(Long id, User actor) {
    Task t = entity(id);
    checkOwner(t, actor);
    return TaskResponse.from(t);
  }

  @Transactional(readOnly = true)
  public Page<TaskResponse> getAll(
      Status status, Priority priority, int page, int size, String sort, User actor) {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("page >= 0; size от 1 до 100");
    String[] parts = sort.split(",", -1);
    if (parts.length > 2
        || !Set.of("id", "title", "priority", "status", "createdAt").contains(parts[0]))
      throw new IllegalArgumentException("Недопустимое поле сортировки");
    Sort.Direction direction =
        parts.length == 2 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, parts[0]).and(Sort.by("id")));
    Page<Task> result;
    if (isAdmin(actor)) {
      if (status != null && priority != null)
        result = taskRepository.findByStatusAndPriority(status, priority, pageable);
      else if (status != null) result = taskRepository.findByStatus(status, pageable);
      else if (priority != null) result = taskRepository.findByPriority(priority, pageable);
      else result = taskRepository.findAll(pageable);
    } else {
      requireActor(actor);
      if (status != null && priority != null)
        result = taskRepository.findByOwnerAndStatusAndPriority(actor, status, priority, pageable);
      else if (status != null)
        result = taskRepository.findByOwnerAndStatus(actor, status, pageable);
      else if (priority != null)
        result = taskRepository.findByOwnerAndPriority(actor, priority, pageable);
      else result = taskRepository.findByOwner(actor, pageable);
    }
    return result.map(TaskResponse::from);
  }

  public TaskResponse create(TaskRequest request, User actor) {
    requireActor(actor);
    Task task = new Task();
    apply(task, request);
    task.setOwner(actor);
    task = taskRepository.save(task);
    auditService.logAction("CREATE", task.getId());
    return TaskResponse.from(task);
  }

  public Optional<TaskResponse> update(Long id, TaskRequest request, User actor) {
    return taskRepository
        .findById(id)
        .map(
            task -> {
              checkOwner(task, actor);
              apply(task, request);
              return TaskResponse.from(taskRepository.save(task));
            });
  }

  public TaskResponse updateStatus(Long id, Status status, User actor) {
    Task task = entity(id);
    checkOwner(task, actor);
    task.setStatus(Objects.requireNonNull(status));
    return TaskResponse.from(taskRepository.save(task));
  }

  public void delete(Long id, User actor) {
    Task task = entity(id);
    checkOwner(task, actor);
    auditService.logAction("DELETE", id);
    taskRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> my(User actor) {
    requireActor(actor);
    return taskRepository.findAllByOwner(actor).stream().map(TaskResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Long> stats(User actor) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (Status s : Status.values()) result.put(s.name(), 0L);
    for (Object[] row : taskRepository.countGroupedByStatus(scope(actor)))
      result.put(((Status) row[0]).name(), ((Number) row[1]).longValue());
    return result;
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> search(String keyword, User actor) {
    return taskRepository.searchByKeyword(keyword, scope(actor)).stream()
        .map(TaskResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> today(User actor) {
    var start = LocalDate.now().atStartOfDay();
    return taskRepository.createdBetween(start, start.plusDays(1), scope(actor)).stream()
        .map(TaskResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean isOwner(Long id, String email) {
    return taskRepository
        .findById(id)
        .filter(t -> t.getOwner() != null && t.getOwner().getEmail().equals(email))
        .isPresent();
  }

  private Task entity(Long id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new TaskNotFoundException("Задача " + id + " не найдена"));
  }

  private Long scope(User actor) {
    requireActor(actor);
    return isAdmin(actor) ? null : actor.getId();
  }

  private boolean isAdmin(User actor) {
    return actor != null && actor.getRole() == Role.ROLE_ADMIN;
  }

  private void requireActor(User actor) {
    if (actor == null) throw new AccessDeniedException("Требуется пользователь");
  }

  private void checkOwner(Task task, User actor) {
    requireActor(actor);
    if (!isAdmin(actor)
        && (task.getOwner() == null || !Objects.equals(task.getOwner().getId(), actor.getId())))
      throw new AccessDeniedException("Not your task");
  }

  private void apply(Task task, TaskRequest request) {
    if (request.getTitle() == null || request.getTitle().isBlank())
      throw new IllegalArgumentException("Название обязательно");
    task.setTitle(request.getTitle().strip());
    task.setDescription(request.getDescription());
    task.setPriority(Objects.requireNonNull(request.getPriority()));
    task.setStatus(Objects.requireNonNull(request.getStatus()));
  }
}
