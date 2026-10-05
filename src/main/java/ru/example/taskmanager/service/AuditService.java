package ru.example.taskmanager.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ru.example.taskmanager.model.AuditLog;
import ru.example.taskmanager.repository.AuditLogRepository;

@Service
public class AuditService {
  private final AuditLogRepository repository;

  public AuditService(AuditLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logAction(String action, Long taskId) {
    AuditLog log = new AuditLog();
    log.setAction(action);
    log.setTaskId(taskId);
    repository.saveAndFlush(log);
  }
}
