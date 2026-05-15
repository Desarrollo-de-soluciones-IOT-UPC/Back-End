package com.emsafe.auth.dto;

public record RefreshResponse(String token, long expiresIn) {}
