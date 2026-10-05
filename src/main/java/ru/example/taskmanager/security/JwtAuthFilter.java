package ru.example.taskmanager.security;

import io.jsonwebtoken.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final UserDetailsService users;
  private final SecurityExceptionHandler errors;

  public JwtAuthFilter(JwtService jwt, UserDetailsService users, SecurityExceptionHandler errors) {
    this.jwt = jwt;
    this.users = users;
    this.errors = errors;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String header = req.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(req, res);
      return;
    }
    try {
      String token = header.substring(7);
      String email = jwt.extractUsername(token);
      if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails user = users.loadUserByUsername(email);
        if (!jwt.isTokenValid(token, user)) {
          errors.write(res, 401, "Invalid token");
          return;
        }
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
      }
    } catch (ExpiredJwtException e) {
      errors.write(res, 401, "Token expired");
      return;
    } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
      errors.write(res, 401, "Invalid token");
      return;
    }
    chain.doFilter(req, res);
  }
}
