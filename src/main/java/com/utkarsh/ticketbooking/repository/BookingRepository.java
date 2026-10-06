package com.utkarsh.ticketbooking.repository;

import com.utkarsh.ticketbooking.model.Booking;
import com.utkarsh.ticketbooking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    // used to stop the same user from booking the same event twice
    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, BookingStatus status);

    long countByEventIdAndStatus(Long eventId, BookingStatus status);

    void deleteByEventId(Long eventId);

    // Marks a booking as cancelled only if it is still confirmed.
    // Returns 0 if it was already cancelled, so a seat is never given back twice.
    @Modifying
    @Query("UPDATE Booking b SET b.status = com.utkarsh.ticketbooking.model.BookingStatus.CANCELLED "
            + "WHERE b.id = :bookingId AND b.status = com.utkarsh.ticketbooking.model.BookingStatus.CONFIRMED")
    int cancelIfConfirmed(@Param("bookingId") Long bookingId);
}
