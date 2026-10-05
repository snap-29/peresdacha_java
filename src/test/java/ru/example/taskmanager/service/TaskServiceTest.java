package ru.example.taskmanager.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import ru.example.taskmanager.dto.*;
import ru.example.taskmanager.exception.TaskNotFoundException;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
  @Mock TaskRepository taskRepository;
  @Mock AuditService auditService;
  @InjectMocks TaskService taskService;

  private User user(Long id, Role role) {
    User u = new User();
    u.setId(id);
    u.setEmail("user" + id + "@test.com");
    u.setRole(role);
    return u;
  }

  private Task buildTask(Long id, String title, Priority priority, Status status) {
    Task t = new Task();
    t.setId(id);
    t.setTitle(title);
    t.setPriority(priority);
    t.setStatus(status);
    t.setOwner(user(1L, Role.ROLE_USER));
    return t;
  }

  private TaskRequest buildRequest(String title, Priority priority) {
    return new TaskRequest(title, "description", priority, Status.NEW);
  }

  @Test
  void getAll_shouldReturnAllTasksAsDtos() {
    // Arrange
    when(taskRepository.findAll())
        .thenReturn(List.of(buildTask(1L, "Buy milk", Priority.HIGH, Status.NEW)));
    // Act
    var result = taskService.getAll();
    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Buy milk");
    assertThat(result.get(0).priority()).isEqualTo("HIGH");
  }

  @Test
  void getAll_whenEmpty_shouldReturnEmptyList() {
    when(taskRepository.findAll()).thenReturn(List.of());
    assertThat(taskService.getAll()).isEmpty();
  }

  @Test
  void create_shouldSaveTaskWithStatusNew() {
    when(taskRepository.save(any()))
        .thenAnswer(
            i -> {
              Task t = i.getArgument(0);
              t.setId(1L);
              return t;
            });
    var result = taskService.create(buildRequest("New", Priority.HIGH), user(1L, Role.ROLE_USER));
    assertThat(result.status()).isEqualTo("NEW");
    assertThat(result.ownerId()).isEqualTo(1L);
  }

  @Test
  void create_shouldCallRepositorySave() {
    when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    taskService.create(buildRequest("New", Priority.LOW), user(1L, Role.ROLE_USER));
    verify(taskRepository).save(any(Task.class));
    verify(auditService).logAction(eq("CREATE"), any());
  }

  @Test
  void create_shouldSetCurrentUserAsOwner() {
    when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    User actor = user(42L, Role.ROLE_USER);
    taskService.create(buildRequest("Task", Priority.HIGH), actor);
    var captor = ArgumentCaptor.forClass(Task.class);
    verify(taskRepository).save(captor.capture());
    assertThat(captor.getValue().getOwner()).isSameAs(actor);
  }

  @Test
  void create_whenTitleBlank_shouldNotCallRepository() {
    assertThatThrownBy(
            () -> taskService.create(buildRequest(" ", Priority.LOW), user(1L, Role.ROLE_USER)))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(taskRepository, auditService);
  }

  @Test
  void findById_whenExists_shouldReturnTask() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Task", Priority.LOW, Status.NEW)));
    assertThat(taskService.findById(1L)).isPresent();
  }

  @Test
  void findById_whenNotExists_shouldReturnEmpty() {
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());
    assertThat(taskService.findById(999L)).isEmpty();
  }

  @Test
  void getOrThrow_whenNotExists_shouldThrowNotFoundException() {
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskService.getOrThrow(999L))
        .isInstanceOf(TaskNotFoundException.class)
        .hasMessageContaining("999");
  }

  @Test
  void update_whenExists_shouldReturnUpdatedTask() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Old", Priority.LOW, Status.DONE)));
    when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    assertThat(
            taskService.update(
                1L, buildRequest("Changed", Priority.HIGH), user(1L, Role.ROLE_USER)))
        .get()
        .extracting(TaskResponse::title)
        .isEqualTo("Changed");
  }

  @Test
  void update_whenNotExists_shouldReturnEmpty() {
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());
    assertThat(
            taskService.update(999L, buildRequest("New", Priority.LOW), user(1L, Role.ROLE_USER)))
        .isEmpty();
    verify(taskRepository, never()).save(any());
  }

  @Test
  void update_otherOwner_shouldThrowAccessDenied() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Old", Priority.LOW, Status.NEW)));
    assertThatThrownBy(
            () ->
                taskService.update(
                    1L, buildRequest("New", Priority.HIGH), user(2L, Role.ROLE_USER)))
        .isInstanceOf(AccessDeniedException.class);
    verify(taskRepository, never()).save(any());
  }

  @Test
  void delete_shouldCallDeleteById() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Task", Priority.LOW, Status.NEW)));
    taskService.delete(1L, user(1L, Role.ROLE_USER));
    verify(taskRepository).deleteById(1L);
    verify(auditService).logAction("DELETE", 1L);
  }

  @Test
  void delete_otherOwner_shouldThrowAccessDenied() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Task", Priority.LOW, Status.NEW)));
    assertThatThrownBy(() -> taskService.delete(1L, user(2L, Role.ROLE_USER)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Not your task");
    verify(taskRepository, never()).deleteById(any());
  }

  @Test
  void delete_adminMayDeleteAnotherUsersTask() {
    when(taskRepository.findById(1L))
        .thenReturn(Optional.of(buildTask(1L, "Task", Priority.LOW, Status.NEW)));
    taskService.delete(1L, user(2L, Role.ROLE_ADMIN));
    verify(taskRepository).deleteById(1L);
  }

  @Test
  void delete_notFound_shouldNotWriteAudit() {
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> taskService.delete(999L, user(1L, Role.ROLE_ADMIN)))
        .isInstanceOf(TaskNotFoundException.class);
    verifyNoInteractions(auditService);
  }

  @Test
  void updateStatus_whenExists_shouldUpdateStatusOnly() {
    Task t = buildTask(1L, "Original", Priority.HIGH, Status.NEW);
    when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
    when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    var result = taskService.updateStatus(1L, Status.DONE, user(1L, Role.ROLE_USER));
    assertThat(result.status()).isEqualTo("DONE");
    assertThat(result.title()).isEqualTo("Original");
    assertThat(result.priority()).isEqualTo("HIGH");
  }

  @Test
  void filteredPage_adminUsesCombinedDerivedQuery() {
    when(taskRepository.findByStatusAndPriority(
            eq(Status.NEW), eq(Priority.HIGH), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    assertThat(
            taskService
                .getAll(Status.NEW, Priority.HIGH, 0, 3, "id", user(1L, Role.ROLE_ADMIN))
                .getContent())
        .isEmpty();
    verify(taskRepository)
        .findByStatusAndPriority(eq(Status.NEW), eq(Priority.HIGH), any(Pageable.class));
  }

  @Test
  void filteredPage_userIsScopedToOwner() {
    User actor = user(1L, Role.ROLE_USER);
    when(taskRepository.findByOwner(eq(actor), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    taskService.getAll(null, null, 0, 10, "id", actor);
    verify(taskRepository).findByOwner(eq(actor), any(Pageable.class));
    verify(taskRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void pagination_invalidSize_shouldFailBeforeRepository() {
    assertThatThrownBy(
            () -> taskService.getAll(null, null, 0, 101, "id", user(1L, Role.ROLE_ADMIN)))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(taskRepository);
  }

  @Test
  void stats_shouldIncludeZeroCounts() {
    when(taskRepository.countGroupedByStatus(null))
        .thenReturn(Collections.singletonList(new Object[] {Status.NEW, 2L}));
    assertThat(taskService.stats(user(1L, Role.ROLE_ADMIN)))
        .containsEntry("NEW", 2L)
        .containsEntry("DONE", 0L);
  }
}
