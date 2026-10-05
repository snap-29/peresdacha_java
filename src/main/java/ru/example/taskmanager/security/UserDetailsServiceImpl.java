package ru.example.taskmanager.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import ru.example.taskmanager.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  private final UserRepository users;

  public UserDetailsServiceImpl(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    return users
        .findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT))
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
