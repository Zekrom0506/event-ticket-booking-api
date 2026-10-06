package com.utkarsh.ticketbooking.controller;

import com.utkarsh.ticketbooking.dto.EventRequest;
import com.utkarsh.ticketbooking.dto.EventResponse;
import com.utkarsh.ticketbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // only organisers can publish events
    @PostMapping
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request,
                                                     Authentication authentication) {
        // authentication.getName() gives the email of the logged in user
        EventResponse response = eventService.createEvent(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<List<EventResponse>> getMyEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyEvents(authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
                                                     @Valid @RequestBody EventRequest request,
                                                     Authentication authentication) {
        EventResponse response = eventService.updateEvent(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        eventService.deleteEvent(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
