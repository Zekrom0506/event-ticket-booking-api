package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.EventRequest;
import com.utkarsh.ticketbooking.dto.EventResponse;
import com.utkarsh.ticketbooking.exception.ResourceNotFoundException;
import com.utkarsh.ticketbooking.model.Event;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.EventRepository;
import com.utkarsh.ticketbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse createEvent(EventRequest request, String organiserEmail) {
        User organiser = findUserByEmail(organiserEmail);

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setTotalSeats(request.getTotalSeats());
        // at the start every seat is available
        event.setAvailableSeats(request.getTotalSeats());
        event.setOrganiser(organiser);

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        List<EventResponse> responses = new ArrayList<>();
        for (Event event : eventRepository.findAll()) {
            responses.add(toResponse(event));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = findEventById(eventId);
        return toResponse(event);
    }

    // events created by the logged in organiser
    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(String organiserEmail) {
        User organiser = findUserByEmail(organiserEmail);

        List<EventResponse> responses = new ArrayList<>();
        for (Event event : eventRepository.findByOrganiserId(organiser.getId())) {
            responses.add(toResponse(event));
        }
        return responses;
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // convert the entity into the object we send back to the client
    private EventResponse toResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setVenue(event.getVenue());
        response.setEventDate(event.getEventDate());
        response.setTotalSeats(event.getTotalSeats());
        response.setAvailableSeats(event.getAvailableSeats());
        response.setOrganiserName(event.getOrganiser().getName());
        return response;
    }
}
