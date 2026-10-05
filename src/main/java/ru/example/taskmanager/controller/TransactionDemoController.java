package ru.example.taskmanager.controller;

import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.example.taskmanager.service.TransactionDemoService;

@RestController
@Profile("tx-demo")
@RequestMapping("/demo/transactions")
@PreAuthorize("hasRole('ADMIN')")
public class TransactionDemoController {
  private final TransactionDemoService demo;

  public TransactionDemoController(TransactionDemoService demo) {
    this.demo = demo;
  }

  @PostMapping("/checked-commit")
  public void checkedCommit() throws IOException {
    demo.checkedCommits();
  }

  @PostMapping("/checked-rollback")
  public void checkedRollback() throws IOException {
    demo.checkedRollsBack();
  }

  @PostMapping("/runtime-rollback")
  public void runtimeRollback() {
    demo.runtimeRollsBack();
  }

  @PostMapping("/audit-rollback")
  public void auditRollback() {
    demo.auditSurvives();
  }
}
