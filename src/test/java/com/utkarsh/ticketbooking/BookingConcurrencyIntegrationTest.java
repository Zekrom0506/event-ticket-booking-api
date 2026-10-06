package com.utkarsh.ticketbooking;

import com.utkarsh.ticketbooking.exception.SoldOutException;
import com.utkarsh.ticketbooking.model.BookingStatus;
import com.utkarsh.ticketbooking.model.Event;
import com.utkarsh.ticketbooking.model.Role;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.BookingRepository;
import com.utkarsh.ticketbooking.repository.EventRepository;
import com.utkarsh.ticketbooking.repository.UserRepository;
import com.utkarsh.ticketbooking.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Integration test: starts the whole application and uses a real PostgreSQL database.
// It checks that the API never sells more tickets than there are seats,
// even when many people try to book at exactly the same time.
@SpringBootTest
class BookingConcurrencyIntegrationTest {

    private static final int TOTAL_SEATS = 50;
    private static final int TOTAL_ATTEMPTS = 100;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Long eventId;
    private List<String> attendeeEmails;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        User organiser = userRepository.save(
                new User("Organiser", "organiser@test.com", "not-used", Role.ORGANISER));

        Event event = new Event();
        event.setTitle("Concurrency Concert");
        event.setVenue("Test Arena");
        event.setEventDate(LocalDateTime.now().plusDays(30));
        event.setTotalSeats(TOTAL_SEATS);
        event.setAvailableSeats(TOTAL_SEATS);
        event.setOrganiser(organiser);
        eventId = eventRepository.save(event).getId();

        // 100 different attendees, one booking attempt each
        attendeeEmails = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ATTEMPTS; i++) {
            String email = "attendee" + i + "@test.com";
            userRepository.save(new User("Attendee " + i, email, "not-used", Role.ATTENDEE));
            attendeeEmails.add(email);
        }
    }

    @AfterEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void hundredConcurrentBookingsForFiftySeatsExactlyFiftySucceed() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_ATTEMPTS);

        // all threads wait on this latch so that they start booking at the same moment
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(TOTAL_ATTEMPTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger soldOutCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        for (String email : attendeeEmails) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    bookingService.bookEvent(eventId, email);
                    successCount.incrementAndGet();
                } catch (SoldOutException e) {
                    soldOutCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        // release all 100 threads together and wait for them to finish
        startSignal.countDown();
        boolean allFinished = finished.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(allFinished, "All booking attempts should finish in time");

        // exactly 50 succeed, the other 50 are told the event is sold out
        assertEquals(TOTAL_SEATS, successCount.get());
        assertEquals(TOTAL_ATTEMPTS - TOTAL_SEATS, soldOutCount.get());
        assertEquals(0, otherErrorCount.get());

        // the database must agree: 50 confirmed bookings and no seats left (not negative)
        long confirmedBookings = bookingRepository.countByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
        assertEquals(TOTAL_SEATS, confirmedBookings);

        Event eventAfter = eventRepository.findById(eventId).orElseThrow();
        assertEquals(0, eventAfter.getAvailableSeats());
    }
}
