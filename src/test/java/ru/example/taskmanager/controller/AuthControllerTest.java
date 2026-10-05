package ru.example.taskmanager.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.taskmanager.config.SecurityConfig;
import ru.example.taskmanager.dto.AuthResponse;
import ru.example.taskmanager.exception.DuplicateEmailException;
import ru.example.taskmanager.security.*;
import ru.example.taskmanager.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class, JwtAuthFilter.class})
class AuthControllerTest {
  @Autowired MockMvc mvc;
  @MockBean AuthService service;
  @MockBean JwtService jwt;
  @MockBean UserDetailsServiceImpl users;
  private static final String BODY = "{\"email\":\"alice@test.com\",\"password\":\"Password123!\"}";

  @Test
  void register_withValidData_shouldReturn200AndToken() throws Exception {
    when(service.register(any())).thenReturn(new AuthResponse("jwt", "jwt", "refresh"));
    mvc.perform(post("/api/auth/register").contentType("application/json").content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("token").value("jwt"))
        .andExpect(jsonPath("refreshToken").value("refresh"));
  }

  @Test
  void register_withDuplicateEmail_shouldReturn409() throws Exception {
    when(service.register(any()))
        .thenThrow(new DuplicateEmailException("Email already registered"));
    mvc.perform(post("/auth/register").contentType("application/json").content(BODY))
        .andExpect(status().isConflict());
  }

  @Test
  void register_withShortPassword_shouldReturn400() throws Exception {
    mvc.perform(
            post("/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"alice@test.com\",\"password\":\"123\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void login_withValidCredentials_shouldReturn200AndToken() throws Exception {
    when(service.login(any())).thenReturn(new AuthResponse("jwt", "jwt", "refresh"));
    mvc.perform(post("/auth/login").contentType("application/json").content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("accessToken").value("jwt"));
  }

  @Test
  void login_withWrongPassword_shouldReturn401() throws Exception {
    when(service.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));
    mvc.perform(post("/auth/login").contentType("application/json").content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_withValidToken_shouldReturn200() throws Exception {
    when(service.refresh(any())).thenReturn(new AuthResponse("new-jwt", "new-jwt", "new-refresh"));
    mvc.perform(
            post("/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"old\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("refreshToken").value("new-refresh"));
  }

  @Test
  void refresh_withExpiredToken_shouldReturn401() throws Exception {
    when(service.refresh(any())).thenThrow(new BadCredentialsException("Expired"));
    mvc.perform(
            post("/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"old\"}"))
        .andExpect(status().isUnauthorized());
  }
}
