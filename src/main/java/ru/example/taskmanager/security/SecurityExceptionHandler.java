package ru.example.taskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper mapper;

  public SecurityExceptionHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public void write(HttpServletResponse response, int status, String error) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    mapper.writeValue(response.getOutputStream(), Map.of("status", status, "error", error));
  }

  @Override
  public void commence(HttpServletRequest r, HttpServletResponse s, AuthenticationException e)
      throws IOException {
    write(s, 401, "Unauthorized");
  }

  @Override
  public void handle(HttpServletRequest r, HttpServletResponse s, AccessDeniedException e)
      throws IOException {
    write(s, 403, "Forbidden");
  }
}
