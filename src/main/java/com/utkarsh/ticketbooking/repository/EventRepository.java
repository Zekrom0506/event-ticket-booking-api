package com.utkarsh.ticketbooking.repository;

import com.utkarsh.ticketbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganiserId(Long organiserId);
}
