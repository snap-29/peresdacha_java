package ru.example.taskmanager.service;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.*;
import ru.example.taskmanager.repository.*;

@DataJpaTest(
    properties = {
      "spring.flyway.enabled=false",
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Import({TransactionDemoService.class, AuditService.class})
@ActiveProfiles("tx-demo")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransactionBehaviorTest {
  @Autowired TransactionDemoService demo;
  @Autowired TaskRepository tasks;
  @Autowired AuditLogRepository audit;

  @BeforeEach
  void clean() {
    tasks.deleteAll();
    audit.deleteAll();
  }

  @Test
  void checkedException_withoutRollbackFor_shouldCommit() {
    assertThatThrownBy(() -> demo.checkedCommits()).isInstanceOf(IOException.class);
    assertThat(tasks.findAll()).extracting(t -> t.getTitle()).containsExactly("checked-commits");
  }

  @Test
  void checkedException_withRollbackFor_shouldRollback() {
    assertThatThrownBy(() -> demo.checkedRollsBack()).isInstanceOf(IOException.class);
    assertThat(tasks.count()).isZero();
  }

  @Test
  void create_whenExceptionAfterSave_shouldRollback() {
    assertThatThrownBy(() -> demo.runtimeRollsBack()).isInstanceOf(IllegalStateException.class);
    assertThat(tasks.count()).isZero();
  }

  @Test
  void auditLog_savedEvenWhenMainTransactionRolledBack() {
    assertThatThrownBy(() -> demo.auditSurvives()).isInstanceOf(IllegalStateException.class);
    assertThat(tasks.count()).isZero();
    assertThat(audit.findAll())
        .singleElement()
        .satisfies(log -> assertThat(log.getAction()).isEqualTo("CREATE_ATTEMPT"));
  }
}
