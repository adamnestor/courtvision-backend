package com.adamnestor.courtvision.service.notification;

import org.springframework.stereotype.Service;
import com.adamnestor.courtvision.model.IncidentTicket;

@Service
public interface TicketingService {
    void createIncident(IncidentTicket ticket);
} 