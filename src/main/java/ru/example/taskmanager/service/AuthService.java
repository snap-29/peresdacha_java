package ru.example.taskmanager.service;

import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.taskmanager.dto.*;
import ru.example.taskmanager.exception.DuplicateEmailException;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.repository.UserRepository;
import ru.example.taskmanager.security.JwtService;

@Service
@Transactional
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthenticationManager manager;
  private final JwtService jwt;
  private final RefreshTokenService refresh;

  public AuthService(
      UserRepository users,
      PasswordEncoder encoder,
      AuthenticationManager manager,
      JwtService jwt,
      RefreshTokenService refresh) {
    this.users = users;
    this.encoder = encoder;
    this.manager = manager;
    this.jwt = jwt;
    this.refresh = refresh;
  }

  public AuthResponse register(RegisterRequest request) {
    String email = normalize(request.email());
    if (users.existsByEmail(email)) throw new DuplicateEmailException("Email already registered");
    User user = new User();
    user.setEmail(email);
    user.setPassword(encoder.encode(request.password()));
    // Registration never accepts a role supplied by a client.
    users.saveAndFlush(user);
    return issue(user);
  }

  public AuthResponse login(LoginRequest request) {
    var auth =
        manager.authenticate(
            new UsernamePasswordAuthenticationToken(
                normalize(request.email()), request.password()));
    return issue((User) auth.getPrincipal());
  }

  public AuthResponse refresh(RefreshRequest request) {
    RefreshToken token = refresh.rotate(request.refreshToken());
    String access = jwt.generateToken(token.getUser());
    return new AuthResponse(access, access, token.getToken());
  }

  public void logout(User user) {
    if (user == null) throw new BadCredentialsException("Unauthorized");
    refresh.deleteByUserId(user.getId());
  }

  private AuthResponse issue(User user) {
    String access = jwt.generateToken(user);
    return new AuthResponse(access, access, refresh.create(user).getToken());
  }

  private String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
