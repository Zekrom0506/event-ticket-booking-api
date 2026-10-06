package com.utkarsh.ticketbooking.repository;

import com.utkarsh.ticketbooking.model.Booking;
import com.utkarsh.ticketbooking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    // used to stop the same user from booking the same event twice
    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, BookingStatus status);

    long countByEventIdAndStatus(Long eventId, BookingStatus status);
}
