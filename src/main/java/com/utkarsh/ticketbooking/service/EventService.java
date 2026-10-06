package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.EventRequest;
import com.utkarsh.ticketbooking.dto.EventResponse;
import com.utkarsh.ticketbooking.exception.BadRequestException;
import com.utkarsh.ticketbooking.exception.ResourceNotFoundException;
import com.utkarsh.ticketbooking.model.BookingStatus;
import com.utkarsh.ticketbooking.model.Event;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.BookingRepository;
import com.utkarsh.ticketbooking.repository.EventRepository;
import com.utkarsh.ticketbooking.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        BookingRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
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

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, String organiserEmail) {
        Event event = findEventById(eventId);
        checkOwnership(event, organiserEmail);

        // Changing the seat count after creation is not allowed. It keeps the seat
        // counter simple, because only bookings and cancellations ever change it.
        if (request.getTotalSeats() != event.getTotalSeats()) {
            throw new BadRequestException("Total seats cannot be changed after the event is created");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void deleteEvent(Long eventId, String organiserEmail) {
        Event event = findEventById(eventId);
        checkOwnership(event, organiserEmail);

        long confirmedBookings = bookingRepository.countByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
        if (confirmedBookings > 0) {
            throw new BadRequestException("Cannot delete an event that has confirmed bookings");
        }

        // remove old cancelled bookings first, otherwise the foreign key blocks the delete
        bookingRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    // An organiser is only allowed to change events that they created
    private void checkOwnership(Event event, String organiserEmail) {
        String ownerEmail = event.getOrganiser().getEmail();
        if (!ownerEmail.equals(organiserEmail)) {
            throw new AccessDeniedException("You can only modify your own events");
        }
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
