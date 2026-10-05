package ru.example.taskmanager.service;

import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.taskmanager.model.Task;
import ru.example.taskmanager.repository.TaskRepository;

@Service
@Profile("tx-demo")
public class TransactionDemoService {
  private final TaskRepository tasks;
  private final AuditService audit;

  public TransactionDemoService(TaskRepository tasks, AuditService audit) {
    this.tasks = tasks;
    this.audit = audit;
  }

  @Transactional
  public void checkedCommits() throws IOException {
    save("checked-commits");
    throw new IOException("Checked exception: committed");
  }

  @Transactional(rollbackFor = Exception.class)
  public void checkedRollsBack() throws IOException {
    save("checked-rollback");
    throw new IOException("Checked exception: rolled back");
  }

  @Transactional
  public void runtimeRollsBack() {
    save("runtime-rollback");
    throw new IllegalStateException("Runtime exception: rolled back");
  }

  @Transactional
  public void auditSurvives() {
    Task task = save("audit-rollback");
    audit.logAction("CREATE_ATTEMPT", task.getId());
    throw new IllegalStateException("Task rolled back; audit committed");
  }

  private Task save(String title) {
    Task task = new Task();
    task.setTitle(title);
    return tasks.saveAndFlush(task);
  }
}
