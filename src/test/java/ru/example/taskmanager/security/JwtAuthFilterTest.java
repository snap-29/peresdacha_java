package ru.example.taskmanager.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;

class JwtAuthFilterTest {
  private final JwtService jwt = mock(JwtService.class);
  private final UserDetailsService users = mock(UserDetailsService.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final JwtAuthFilter filter =
      new JwtAuthFilter(jwt, users, new SecurityExceptionHandler(new ObjectMapper()));

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void missingHeader_shouldContinueChain() throws Exception {
    var req = new MockHttpServletRequest();
    var res = new MockHttpServletResponse();
    filter.doFilter(req, res, chain);
    verify(chain).doFilter(req, res);
    verifyNoInteractions(jwt);
  }

  @Test
  void expiredToken_shouldReturn401() throws Exception {
    var req = new MockHttpServletRequest();
    req.addHeader("Authorization", "Bearer expired");
    var res = new MockHttpServletResponse();
    when(jwt.extractUsername("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));
    filter.doFilter(req, res, chain);
    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Token expired");
    verifyNoInteractions(chain);
  }

  @Test
  void validToken_shouldAuthenticateAndContinue() throws Exception {
    var req = new MockHttpServletRequest();
    req.addHeader("Authorization", "Bearer valid");
    var res = new MockHttpServletResponse();
    UserDetails user = User.withUsername("alice@test.com").password("unused").roles("USER").build();
    when(jwt.extractUsername("valid")).thenReturn(user.getUsername());
    when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
    when(jwt.isTokenValid("valid", user)).thenReturn(true);
    filter.doFilter(req, res, chain);
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isSameAs(user);
    verify(chain).doFilter(req, res);
  }
}
