package ru.example.taskmanager;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired RefreshTokenRepository refresh;
  @Autowired PasswordEncoder encoder;

  private String email() {
    return UUID.randomUUID() + "@test.com";
  }

  private JsonNode register(String email) throws Exception {
    return mapper.readTree(
        mvc.perform(
                post("/auth/register")
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            java.util.Map.of("email", email, "password", "Password123!"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private long create(String token, String title) throws Exception {
    return mapper
        .readTree(
            mvc.perform(
                    post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(
                            mapper.writeValueAsString(
                                java.util.Map.of("title", title, "priority", "HIGH"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asLong();
  }

  @Test
  void register_login_getAll_shouldWork() throws Exception {
    String mail = email();
    register(mail);
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            java.util.Map.of("email", mail, "password", "Password123!"))))
            .andExpect(status().isOk())
            .andReturn();
    String token = mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void userDelete_shouldReturn403_evenForOwnTask() throws Exception {
    String token = register(email()).get("token").asText();
    long id = create(token, "own");
    mvc.perform(delete("/tasks/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void duplicateRegistration_shouldReturn409() throws Exception {
    String mail = email();
    register(mail);
    mvc.perform(
            post("/auth/register")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(
                        java.util.Map.of("email", mail, "password", "Password123!"))))
        .andExpect(status().isConflict());
  }

  @Test
  void myTasksAndAllQueries_shouldNotLeakOtherOwners() throws Exception {
    String alice = register(email()).get("token").asText(),
        bob = register(email()).get("token").asText();
    long own = create(alice, "Private marker");
    long other = create(bob, "Private marker");
    mvc.perform(get("/tasks/my").header("Authorization", "Bearer " + alice))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(own));
    mvc.perform(get("/tasks").header("Authorization", "Bearer " + alice))
        .andExpect(jsonPath("totalElements").value(1));
    mvc.perform(get("/tasks/search?keyword=Private").header("Authorization", "Bearer " + alice))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/tasks/stats").header("Authorization", "Bearer " + alice))
        .andExpect(jsonPath("NEW").value(1));
    mvc.perform(
            put("/tasks/" + other)
                .header("Authorization", "Bearer " + alice)
                .contentType("application/json")
                .content("{\"title\":\"hack\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tasks/" + other).header("Authorization", "Bearer " + alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void refresh_shouldRotate_andLogoutShouldInvalidateRefreshTokens() throws Exception {
    var original = register(email());
    String old = original.get("refreshToken").asText();
    var renewed =
        mvc.perform(
                post("/auth/refresh")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", old))))
            .andExpect(status().isOk())
            .andReturn();
    var next = mapper.readTree(renewed.getResponse().getContentAsString());
    assertThat(next.get("refreshToken").asText()).isNotEqualTo(old);
    mvc.perform(
            post("/auth/refresh")
                .contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", old))))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/auth/logout").header("Authorization", "Bearer " + next.get("token").asText()))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/auth/refresh")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(
                        java.util.Map.of("refreshToken", next.get("refreshToken").asText()))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredRefresh_shouldReturn401() throws Exception {
    String mail = email();
    register(mail);
    RefreshToken token = new RefreshToken();
    token.setUser(users.findByEmail(mail).orElseThrow());
    token.setToken(UUID.randomUUID().toString());
    token.setExpiryDate(Instant.now().minusSeconds(1));
    refresh.saveAndFlush(token);
    mvc.perform(
            post("/auth/refresh")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(java.util.Map.of("refreshToken", token.getToken()))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminCrudScenario_shouldHaveCorrectStatuses() throws Exception {
    String mail = email();
    register(mail);
    User admin = users.findByEmail(mail).orElseThrow();
    admin.setRole(Role.ROLE_ADMIN);
    users.saveAndFlush(admin);
    var login =
        mvc.perform(
                post("/auth/login")
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            java.util.Map.of("email", mail, "password", "Password123!"))))
            .andExpect(status().isOk())
            .andReturn();
    String token = mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    long id = create(token, "Crud");
    mvc.perform(
            patch("/tasks/" + id + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status").value("DONE"));
    mvc.perform(delete("/tasks/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/tasks/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void browserLogin_shouldRenderPages_andRequireCsrf() throws Exception {
    String mail = email();
    register(mail);
    mvc.perform(get("/web/tasks")).andExpect(status().is3xxRedirection());
    var result =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", mail)
                    .param("password", "Password123!"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    mvc.perform(get("/web/tasks").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Task Manager")));
    mvc.perform(get("/web/tasks/new").session(session)).andExpect(status().isOk());
    mvc.perform(post("/web/tasks").session(session).param("title", "No csrf"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/web/tasks")
                .session(session)
                .with(csrf())
                .param("title", "Browser task")
                .param("priority", "HIGH")
                .param("status", "NEW"))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get("/web/tasks").session(session))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Browser task")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">Удалить<"))));
    mvc.perform(post("/web/tasks").session(session).with(csrf()).param("title", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Название обязательно")));
  }
}
