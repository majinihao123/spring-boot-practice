 package com.example.demo.auth;

  import jakarta.persistence.Column;
  import jakarta.persistence.Entity;
  import jakarta.persistence.GeneratedValue;
  import jakarta.persistence.GenerationType;
  import jakarta.persistence.Id;
  import jakarta.persistence.Table;

  import java.time.Instant;

  @Entity
  @Table(name = "app_user")
  public class AppUser {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(nullable = false, unique = true, length = 64)
      private String username;

      @Column(name = "password_hash", nullable = false, length = 255)
      private String passwordHash;

      @Column(nullable = false, length = 32)
      private String role;

      @Column(nullable = false)
      private boolean enabled;

      @Column(name = "created_at", insertable = false, updatable = false)
      private Instant createdAt;

      @Column(name = "updated_at", insertable = false, updatable = false)
      private Instant updatedAt;

      protected AppUser() {
      }

      public AppUser(
              String username,
              String passwordHash,
              String role,
              boolean enabled
      ) {
          this.username = username;
          this.passwordHash = passwordHash;
          this.role = role;
          this.enabled = enabled;
      }

      public Long getId() {
          return id;
      }

      public String getUsername() {
          return username;
      }

      public String getPasswordHash() {
          return passwordHash;
      }

      public String getRole() {
          return role;
      }

      public boolean isEnabled() {
          return enabled;
      }

      public Instant getCreatedAt() {
          return createdAt;
      }

      public Instant getUpdatedAt() {
          return updatedAt;
      }
  }