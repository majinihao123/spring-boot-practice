  package com.example.demo.auth;

  public record LoginResponse(
          String tokenType,
          String accessToken,
          long expiresIn
  ) {
  }