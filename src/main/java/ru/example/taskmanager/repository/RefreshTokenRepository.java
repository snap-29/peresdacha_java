package ru.example.taskmanager.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.example.taskmanager.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RefreshToken r join fetch r.user where r.token = :token")
  Optional<RefreshToken> findByToken(@Param("token") String token);

  void deleteByUserId(Long userId);
}
