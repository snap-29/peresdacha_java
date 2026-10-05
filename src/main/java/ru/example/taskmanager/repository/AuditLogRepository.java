package ru.example.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.taskmanager.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
