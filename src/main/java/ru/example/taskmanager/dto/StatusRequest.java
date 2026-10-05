package ru.example.taskmanager.dto;

import jakarta.validation.constraints.*;

public record StatusRequest(@NotNull ru.example.taskmanager.model.Status status) {}
