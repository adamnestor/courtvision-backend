package com.adamnestor.courtvision.model;

import java.util.Set;

public class IncidentTicket {
    private IncidentPriority priority;
    private IncidentCategory category;
    private String description;
    private Set<String> affectedComponents;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IncidentPriority priority;
        private IncidentCategory category;
        private String description;
        private Set<String> affectedComponents;

        public Builder priority(IncidentPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder category(IncidentCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder affectedComponents(Set<String> components) {
            this.affectedComponents = components;
            return this;
        }

        public IncidentTicket build() {
            IncidentTicket ticket = new IncidentTicket();
            ticket.priority = this.priority;
            ticket.category = this.category;
            ticket.description = this.description;
            ticket.affectedComponents = this.affectedComponents;
            return ticket;
        }
    }
} 