package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "players")
public class Players {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Long externalId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Teams team;

    private String position;

    @Column(name = "jersey_number")
    private String jerseyNumber;

    @Enumerated(EnumType.STRING)
    private PlayerStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExternalId() { return externalId; }
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Teams getTeam() { return team; }
    public void setTeam(Teams team) { this.team = team; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(String jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}