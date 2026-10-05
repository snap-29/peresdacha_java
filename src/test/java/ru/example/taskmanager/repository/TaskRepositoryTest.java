package ru.example.taskmanager.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import ru.example.taskmanager.model.*;

@DataJpaTest(
    properties = {
      "spring.flyway.enabled=false",
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class TaskRepositoryTest {
  @Autowired TaskRepository tasks;

  private Task task(String title, Priority p, Status s) {
    Task t = new Task();
    t.setTitle(title);
    t.setPriority(p);
    t.setStatus(s);
    return tasks.saveAndFlush(t);
  }

  @Test
  void save_shouldPersistTask() {
    var saved = task("Test", Priority.HIGH, Status.NEW);
    assertThat(saved.getId()).isNotNull();
    assertThat(tasks.findById(saved.getId())).isPresent();
  }

  @Test
  void combinedDerivedQuery_shouldFilterBothFields() {
    task("A", Priority.HIGH, Status.NEW);
    task("B", Priority.LOW, Status.NEW);
    task("C", Priority.HIGH, Status.DONE);
    assertThat(tasks.findByStatusAndPriority(Status.NEW, Priority.HIGH))
        .extracting(Task::getTitle)
        .containsExactly("A");
  }

  @Test
  void keywordQuery_shouldSearchTitleAndDescriptionIgnoringCase() {
    Task t = task("Other", Priority.HIGH, Status.NEW);
    t.setDescription("Learn JAVA");
    tasks.flush();
    assertThat(tasks.searchByKeyword("java", null))
        .extracting(Task::getId)
        .containsExactly(t.getId());
  }

  @Test
  void pagination_shouldReturnNextPage() {
    for (int i = 0; i < 5; i++) task("Task " + i, Priority.LOW, Status.NEW);
    var page = tasks.findAll(PageRequest.of(1, 3));
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(5);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  @Test
  void groupedStats_shouldCountStatuses() {
    task("A", Priority.HIGH, Status.NEW);
    task("B", Priority.LOW, Status.NEW);
    assertThat(tasks.countGroupedByStatus(null))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row[0]).isEqualTo(Status.NEW);
              assertThat(row[1]).isEqualTo(2L);
            });
  }
}
