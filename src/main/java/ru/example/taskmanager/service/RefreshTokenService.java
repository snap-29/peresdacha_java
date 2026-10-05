package ru.example.taskmanager.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.repository.*;

@Service
@Transactional
public class RefreshTokenService {
  private final RefreshTokenRepository repository;
  private final UserRepository users;
  private final long lifetime;

  public RefreshTokenService(
      RefreshTokenRepository repository,
      UserRepository users,
      @Value("${jwt.refresh-expiration}") long lifetime) {
    this.repository = repository;
    this.users = users;
    this.lifetime = lifetime;
  }

  public RefreshToken createRefreshToken(Long userId) {
    return create(
        users.findById(userId).orElseThrow(() -> new BadCredentialsException("Unknown user")));
  }

  public RefreshToken create(User user) {
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setToken(UUID.randomUUID().toString());
    token.setExpiryDate(Instant.now().plusMillis(lifetime));
    return repository.save(token);
  }

  public RefreshToken verifyExpiration(RefreshToken token) {
    if (!token.getExpiryDate().isAfter(Instant.now()))
      throw new BadCredentialsException("Refresh token expired");
    return token;
  }

  public RefreshToken rotate(String value) {
    RefreshToken old =
        repository
            .findByToken(value)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    verifyExpiration(old);
    User user = old.getUser();
    repository.delete(old);
    repository.flush();
    return create(user);
  }

  public void deleteByUserId(Long userId) {
    repository.deleteByUserId(userId);
  }
}
