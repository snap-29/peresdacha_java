package ru.example.taskmanager.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.taskmanager.config.SecurityConfig;
import ru.example.taskmanager.security.*;
import ru.example.taskmanager.service.TaskService;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class, JwtAuthFilter.class})
class TaskControllerTest {
  @Autowired MockMvc mvc;
  @MockBean TaskService service;
  @MockBean JwtService jwt;
  @MockBean UserDetailsServiceImpl users;

  @Test
  void getAll_withoutToken_shouldReturn401Json() throws Exception {
    mvc.perform(get("/api/tasks"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("error").value("Unauthorized"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_whenUser_shouldReturn200() throws Exception {
    when(service.getAll(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenReturn(Page.empty());
    mvc.perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("content").isArray());
  }

  @Test
  @WithMockUser(roles = "USER")
  void delete_whenRoleUser_shouldReturn403() throws Exception {
    mvc.perform(delete("/api/tasks/1")).andExpect(status().isForbidden());
    verify(service, never()).delete(anyLong(), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_whenAdmin_shouldReturn204() throws Exception {
    mvc.perform(delete("/api/tasks/1")).andExpect(status().isNoContent());
    verify(service).delete(eq(1L), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void post_whenTitleBlank_shouldReturn400() throws Exception {
    mvc.perform(post("/tasks").contentType("application/json").content("{\"title\":\" \"}"))
        .andExpect(status().isBadRequest());
    verify(service, never()).create(any(), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  void patch_whenInvalidStatus_shouldReturn400() throws Exception {
    mvc.perform(
            patch("/tasks/1/status")
                .contentType("application/json")
                .content("{\"status\":\"UNKNOWN\"}"))
        .andExpect(status().isBadRequest());
  }
}
