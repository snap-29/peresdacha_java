package ru.example.taskmanager.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.model.Role;
import ru.example.taskmanager.repository.*;

@Configuration
@ConditionalOnProperty(name = "app.demo-users", havingValue = "true")
public class DemoDataConfig {
  @Bean
  public CommandLineRunner demoUsers(
      UserRepository users, TaskRepository tasks, PasswordEncoder encoder, TransactionTemplate tx) {
    return args ->
        tx.executeWithoutResult(
            status -> {
              User admin = create(users, encoder, "admin@test.com", "Admin123!", Role.ROLE_ADMIN);
              create(users, encoder, "user@test.com", "User123!", Role.ROLE_USER);
              // Assign migration seed tasks to the local demo admin.
              for (Task task : tasks.findAll()) if (task.getOwner() == null) task.setOwner(admin);
            });
  }

  private User create(
      UserRepository users, PasswordEncoder encoder, String email, String password, Role role) {
    return users
        .findByEmail(email)
        .orElseGet(
            () -> {
              User user = new User();
              user.setEmail(email);
              user.setPassword(encoder.encode(password));
              user.setRole(role);
              return users.save(user);
            });
  }
}
