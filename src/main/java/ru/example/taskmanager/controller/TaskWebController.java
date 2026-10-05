package ru.example.taskmanager.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.example.taskmanager.dto.*;
import ru.example.taskmanager.exception.TaskNotFoundException;
import ru.example.taskmanager.model.*;
import ru.example.taskmanager.service.TaskService;

@Controller
@RequestMapping("/web/tasks")
public class TaskWebController {
  private final TaskService service;

  public TaskWebController(TaskService service) {
    this.service = service;
  }

  @ModelAttribute("priorities")
  public Priority[] priorities() {
    return Priority.values();
  }

  @ModelAttribute("statuses")
  public Status[] statuses() {
    return Status.values();
  }

  @GetMapping
  public String list(
      @RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal User user, Model model) {
    var tasks = service.getAll(null, null, page, 10, "id", user);
    model.addAttribute("tasks", tasks.getContent());
    model.addAttribute("page", tasks);
    return "tasks/list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
    model.addAttribute("task", service.getById(id, user));
    return "tasks/detail";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("taskRequest", new TaskRequest());
    return "tasks/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("taskRequest") TaskRequest request,
      BindingResult result,
      @AuthenticationPrincipal User user) {
    if (result.hasErrors()) return "tasks/form";
    service.create(request, user);
    return "redirect:/web/tasks";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
    var task = service.getById(id, user);
    model.addAttribute(
        "taskRequest",
        new TaskRequest(
            task.title(),
            task.description(),
            Priority.valueOf(task.priority()),
            Status.valueOf(task.status())));
    model.addAttribute("taskId", id);
    return "tasks/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("taskRequest") TaskRequest request,
      BindingResult result,
      @AuthenticationPrincipal User user,
      Model model) {
    model.addAttribute("taskId", id);
    if (result.hasErrors()) return "tasks/form";
    service
        .update(id, request, user)
        .orElseThrow(() -> new TaskNotFoundException("Задача " + id + " не найдена"));
    return "redirect:/web/tasks";
  }

  @PostMapping("/{id}/delete")
  @PreAuthorize("hasRole('ADMIN')")
  public String delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
    service.delete(id, user);
    return "redirect:/web/tasks";
  }
}
