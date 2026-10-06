package com.utkarsh.ticketbooking.controller;

import com.utkarsh.ticketbooking.dto.BookingResponse;
import com.utkarsh.ticketbooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // book one seat for the given event
    @PostMapping("/events/{eventId}/bookings")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<BookingResponse> bookEvent(@PathVariable Long eventId,
                                                     Authentication authentication) {
        BookingResponse response = bookingService.bookEvent(eventId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId,
                                                         Authentication authentication) {
        BookingResponse response = bookingService.cancelBooking(bookingId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/mine")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }
}
