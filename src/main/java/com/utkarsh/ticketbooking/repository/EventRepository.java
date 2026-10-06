package com.utkarsh.ticketbooking.repository;

import com.utkarsh.ticketbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganiserId(Long organiserId);

    // Takes one seat, but only if at least one seat is still free.
    // The check (availableSeats > 0) and the update happen in ONE SQL statement,
    // so the database makes sure two requests can never take the same last seat.
    // Returns 1 if a seat was taken and 0 if the event is sold out.
    @Modifying
    @Query("UPDATE Event e SET e.availableSeats = e.availableSeats - 1 "
            + "WHERE e.id = :eventId AND e.availableSeats > 0")
    int decrementAvailableSeats(@Param("eventId") Long eventId);

    // Gives one seat back when a booking is cancelled.
    // The extra condition makes sure we never go above the total seats.
    @Modifying
    @Query("UPDATE Event e SET e.availableSeats = e.availableSeats + 1 "
            + "WHERE e.id = :eventId AND e.availableSeats < e.totalSeats")
    int incrementAvailableSeats(@Param("eventId") Long eventId);
}
