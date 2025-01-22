package com.adamnestor.courtvision.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiTeam {
    private Long id;
    private String conference;
    private String division;
    private String city;
    private String name;
    @JsonProperty("full_name")
    private String fullName;
    private String abbreviation;

    // Constructor
    public ApiTeam() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConference() { return conference; }
    public void setConference(String conference) { this.conference = conference; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
} 