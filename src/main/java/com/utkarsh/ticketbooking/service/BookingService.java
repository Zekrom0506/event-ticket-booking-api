package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.BookingResponse;
import com.utkarsh.ticketbooking.exception.BadRequestException;
import com.utkarsh.ticketbooking.exception.ResourceNotFoundException;
import com.utkarsh.ticketbooking.exception.SoldOutException;
import com.utkarsh.ticketbooking.model.Booking;
import com.utkarsh.ticketbooking.model.BookingStatus;
import com.utkarsh.ticketbooking.model.Event;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.BookingRepository;
import com.utkarsh.ticketbooking.repository.EventRepository;
import com.utkarsh.ticketbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // Books one seat for the logged in user.
    // Everything runs in one transaction, so if saving the booking fails
    // the seat that was taken is given back automatically (rollback).
    @Transactional
    public BookingResponse bookEvent(Long eventId, String userEmail) {
        User user = findUserByEmail(userEmail);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This event has already taken place");
        }

        boolean alreadyBooked = bookingRepository.existsByUserIdAndEventIdAndStatus(
                user.getId(), eventId, BookingStatus.CONFIRMED);
        if (alreadyBooked) {
            throw new BadRequestException("You have already booked this event");
        }

        // This is the important part. We do NOT read the seat count in Java and then
        // write it back, because two requests could read the same value at the same time.
        // Instead the database decreases the count only if a seat is left.
        int updatedRows = eventRepository.decrementAvailableSeats(eventId);
        if (updatedRows == 0) {
            throw new SoldOutException("Sorry, this event is sold out");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setEventId(booking.getEvent().getId());
        response.setEventTitle(booking.getEvent().getTitle());
        response.setStatus(booking.getStatus().name());
        response.setBookedAt(booking.getBookedAt());
        return response;
    }
}
