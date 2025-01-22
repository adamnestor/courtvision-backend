package com.adamnestor.courtvision.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiPlayer {
    private Long id;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    private String position;
    private String height;
    private String weight;
    @JsonProperty("jersey_number")
    private String jerseyNumber;
    private String college;
    private String country;
    @JsonProperty("draft_year")
    private Integer draftYear;
    @JsonProperty("draft_round")
    private Integer draftRound;
    @JsonProperty("draft_number")
    private Integer draftNumber;
    @JsonProperty("team_id")
    private Long teamId;

    // Constructor
    public ApiPlayer() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(String jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getDraftYear() { return draftYear; }
    public void setDraftYear(Integer draftYear) { this.draftYear = draftYear; }

    public Integer getDraftRound() { return draftRound; }
    public void setDraftRound(Integer draftRound) { this.draftRound = draftRound; }

    public Integer getDraftNumber() { return draftNumber; }
    public void setDraftNumber(Integer draftNumber) { this.draftNumber = draftNumber; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
} 