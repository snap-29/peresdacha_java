package ru.example.taskmanager.dto;

import jakarta.validation.constraints.*;

public record AuthResponse(String token, String accessToken, String refreshToken) {}
