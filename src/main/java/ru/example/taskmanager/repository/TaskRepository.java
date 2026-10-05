package ru.example.taskmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.example.taskmanager.model.*;

public interface TaskRepository extends JpaRepository<Task, Long> {
  List<Task> findByStatus(Status status);

  List<Task> findByPriority(Priority priority);

  List<Task> findByStatusAndPriority(Status status, Priority priority);

  List<Task> findByTitleContainingIgnoreCase(String keyword);

  long countByStatus(Status status);

  List<Task> findAllByOwner(User owner);

  Page<Task> findByStatus(Status status, Pageable page);

  Page<Task> findByPriority(Priority priority, Pageable page);

  Page<Task> findByStatusAndPriority(Status status, Priority priority, Pageable page);

  Page<Task> findByOwner(User owner, Pageable page);

  Page<Task> findByOwnerAndStatus(User owner, Status status, Pageable page);

  Page<Task> findByOwnerAndPriority(User owner, Priority priority, Pageable page);

  Page<Task> findByOwnerAndStatusAndPriority(
      User owner, Status status, Priority priority, Pageable page);

  @Query(
      "select t.status, count(t) from Task t where (:ownerId is null or t.owner.id = :ownerId)"
          + " group by t.status")
  List<Object[]> countGroupedByStatus(@Param("ownerId") Long ownerId);

  @Query(
      "select t from Task t where (:ownerId is null or t.owner.id = :ownerId) and (lower(t.title)"
          + " like lower(concat('%', :kw, '%')) or lower(t.description) like lower(concat('%', :kw,"
          + " '%')))")
  List<Task> searchByKeyword(@Param("kw") String keyword, @Param("ownerId") Long ownerId);

  @Query(
      "select t from Task t where t.createdAt >= :start and t.createdAt < :end and (:ownerId is"
          + " null or t.owner.id = :ownerId)")
  List<Task> createdBetween(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("ownerId") Long ownerId);
}
