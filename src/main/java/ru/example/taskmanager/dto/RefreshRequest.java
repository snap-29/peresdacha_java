package ru.example.taskmanager.dto;

import jakarta.validation.constraints.*;

public record RefreshRequest(@NotBlank String refreshToken) {}
