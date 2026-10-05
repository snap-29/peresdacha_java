package ru.example.taskmanager.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {
  static final String SECRET = "bG9jYWwtZGV2LW9ubHktc2VjcmV0LW11c3QtYmUtMzItYnl0ZXMh";
  private final JwtService jwt = new JwtService(SECRET, 86400000);
  private final org.springframework.security.core.userdetails.UserDetails user =
      User.withUsername("alice@test.com").password("unused").roles("USER").build();

  @Test
  void generateToken_shouldContainCorrectUsername() {
    assertThat(jwt.extractUsername(jwt.generateToken(user))).isEqualTo("alice@test.com");
  }

  @Test
  void isTokenValid_correctUser_shouldReturnTrue() {
    assertThat(jwt.isTokenValid(jwt.generateToken(user), user)).isTrue();
  }

  @Test
  void isTokenValid_otherUser_shouldReturnFalse() {
    var other = User.withUsername("bob@test.com").password("unused").roles("USER").build();
    assertThat(jwt.isTokenValid(jwt.generateToken(user), other)).isFalse();
  }

  @Test
  void isTokenValid_expiredToken_shouldReturnFalse() {
    var expired = new JwtService(SECRET, -1000);
    assertThat(jwt.isTokenValid(expired.generateToken(user), user)).isFalse();
  }

  @Test
  void isTokenValid_malformedToken_shouldReturnFalse() {
    assertThat(jwt.isTokenValid("broken", user)).isFalse();
  }

  @Test
  void isTokenValid_wrongSigningKey_shouldReturnFalse() {
    String otherSecret =
        java.util.Base64.getEncoder()
            .encodeToString(
                "a-different-secret-key-of-32-bytes!!"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(jwt.isTokenValid(new JwtService(otherSecret, 10000).generateToken(user), user))
        .isFalse();
  }
}
