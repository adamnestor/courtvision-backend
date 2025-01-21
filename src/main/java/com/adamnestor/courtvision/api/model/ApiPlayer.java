package com.adamnestor.courtvision.api.model;

public class ApiPlayer {
    private Long id;
    private String firstName;
    private String lastName;
    private String position;
    private Integer heightFeet;
    private Integer heightInches;
    private Integer weightPounds;
    private ApiTeam team;

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

    public Integer getHeightFeet() { return heightFeet; }
    public void setHeightFeet(Integer heightFeet) { this.heightFeet = heightFeet; }

    public Integer getHeightInches() { return heightInches; }
    public void setHeightInches(Integer heightInches) { this.heightInches = heightInches; }

    public Integer getWeightPounds() { return weightPounds; }
    public void setWeightPounds(Integer weightPounds) { this.weightPounds = weightPounds; }

    public ApiTeam getTeam() { return team; }
    public void setTeam(ApiTeam team) { this.team = team; }
} 