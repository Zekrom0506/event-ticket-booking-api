package com.utkarsh.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TicketBookingApplication {

    public static void main(String[] args) {
        // Run the app in UTC on every machine. Without this the PostgreSQL driver sends
        // the computer's own time zone name (for example "Asia/Calcutta") to the database,
        // and some PostgreSQL installs reject that old name and refuse the connection.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(TicketBookingApplication.class, args);
    }
}
