package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.EventRequest;
import com.utkarsh.ticketbooking.dto.EventResponse;
import com.utkarsh.ticketbooking.exception.BadRequestException;
import com.utkarsh.ticketbooking.exception.ResourceNotFoundException;
import com.utkarsh.ticketbooking.model.BookingStatus;
import com.utkarsh.ticketbooking.model.Event;
import com.utkarsh.ticketbooking.model.Role;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.BookingRepository;
import com.utkarsh.ticketbooking.repository.EventRepository;
import com.utkarsh.ticketbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private EventService eventService;

    private User owner;
    private Event event;

    @BeforeEach
    void setUp() {
        owner = new User("Owner", "owner@test.com", "hash", Role.ORGANISER);
        owner.setId(1L);

        event = new Event();
        event.setId(10L);
        event.setTitle("Music Night");
        event.setVenue("Main Hall");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalSeats(50);
        event.setAvailableSeats(50);
        event.setOrganiser(owner);
    }

    // small helper to build a request body
    private EventRequest buildRequest(String title, int totalSeats) {
        EventRequest request = new EventRequest();
        request.setTitle(title);
        request.setDescription("Some description");
        request.setVenue("Main Hall");
        request.setEventDate(LocalDateTime.now().plusDays(7));
        request.setTotalSeats(totalSeats);
        return request;
    }

    @Test
    void createEventStartsWithAllSeatsAvailable() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        // return the same event object that the service tries to save
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.createEvent(buildRequest("Music Night", 50), "owner@test.com");

        assertEquals(50, response.getTotalSeats());
        assertEquals(50, response.getAvailableSeats());
        assertEquals("Owner", response.getOrganiserName());
    }

    @Test
    void ownerCanUpdateEvent() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.updateEvent(10L, buildRequest("New Title", 50), "owner@test.com");

        assertEquals("New Title", response.getTitle());
    }

    @Test
    void otherOrganiserCannotUpdateEvent() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class,
                () -> eventService.updateEvent(10L, buildRequest("Hacked", 50), "someone-else@test.com"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void totalSeatsCannotBeChangedOnUpdate() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class,
                () -> eventService.updateEvent(10L, buildRequest("Music Night", 500), "owner@test.com"));
    }

    @Test
    void otherOrganiserCannotDeleteEvent() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class,
                () -> eventService.deleteEvent(10L, "someone-else@test.com"));

        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void eventWithConfirmedBookingsCannotBeDeleted() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(10L, BookingStatus.CONFIRMED)).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> eventService.deleteEvent(10L, "owner@test.com"));

        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void getEventByIdThrowsWhenEventDoesNotExist() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.getEventById(99L));
    }
}
