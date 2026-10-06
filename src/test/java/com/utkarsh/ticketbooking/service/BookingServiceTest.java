package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.BookingResponse;
import com.utkarsh.ticketbooking.exception.BadRequestException;
import com.utkarsh.ticketbooking.exception.SoldOutException;
import com.utkarsh.ticketbooking.model.Booking;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User attendee;
    private Event event;

    @BeforeEach
    void setUp() {
        attendee = new User("Ravi", "ravi@test.com", "hash", Role.ATTENDEE);
        attendee.setId(5L);

        event = new Event();
        event.setId(10L);
        event.setTitle("Music Night");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalSeats(50);
        event.setAvailableSeats(50);
    }

    // a confirmed booking that belongs to the attendee above
    private Booking buildConfirmedBooking() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setUser(attendee);
        booking.setEvent(event);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());
        return booking;
    }

    @Test
    void bookingSucceedsWhenSeatIsAvailable() {
        when(userRepository.findByEmail("ravi@test.com")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventIdAndStatus(5L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(false);
        // 1 row updated means a seat was taken
        when(eventRepository.decrementAvailableSeats(10L)).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.bookEvent(10L, "ravi@test.com");

        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(10L, response.getEventId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void bookingFailsWhenEventIsSoldOut() {
        when(userRepository.findByEmail("ravi@test.com")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        // 0 rows updated means there was no seat left
        when(eventRepository.decrementAvailableSeats(10L)).thenReturn(0);

        assertThrows(SoldOutException.class, () -> bookingService.bookEvent(10L, "ravi@test.com"));

        // no booking should be saved for a sold out event
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void sameUserCannotBookSameEventTwice() {
        when(userRepository.findByEmail("ravi@test.com")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventIdAndStatus(5L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookingService.bookEvent(10L, "ravi@test.com"));

        // the seat count must not be touched
        verify(eventRepository, never()).decrementAvailableSeats(anyLong());
    }

    @Test
    void pastEventCannotBeBooked() {
        event.setEventDate(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail("ravi@test.com")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> bookingService.bookEvent(10L, "ravi@test.com"));
    }

    @Test
    void cancelGivesTheSeatBack() {
        Booking booking = buildConfirmedBooking();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.cancelIfConfirmed(100L)).thenReturn(1);

        BookingResponse response = bookingService.cancelBooking(100L, "ravi@test.com");

        assertEquals("CANCELLED", response.getStatus());
        verify(eventRepository).incrementAvailableSeats(10L);
    }

    @Test
    void userCannotCancelSomeoneElsesBooking() {
        Booking booking = buildConfirmedBooking();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(AccessDeniedException.class,
                () -> bookingService.cancelBooking(100L, "stranger@test.com"));

        verify(eventRepository, never()).incrementAvailableSeats(anyLong());
    }

    @Test
    void cancellingTwiceDoesNotGiveTheSeatBackTwice() {
        Booking booking = buildConfirmedBooking();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        // 0 rows updated means the booking was already cancelled
        when(bookingRepository.cancelIfConfirmed(100L)).thenReturn(0);

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(100L, "ravi@test.com"));

        verify(eventRepository, never()).incrementAvailableSeats(anyLong());
    }
}
