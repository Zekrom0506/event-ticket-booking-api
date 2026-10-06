# Event Ticket Booking API

A REST API where organisers publish events with a limited number of seats, and attendees book or cancel tickets.

**Tech stack:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, JUnit 5, Mockito, Swagger, Docker, GitHub Actions, AWS EC2

## Features

- Register and login with JWT. Passwords are stored as BCrypt hashes.
- Two roles: `ORGANISER` and `ATTENDEE`.
- Organisers can create, update and delete events, but only their own events.
- Attendees can book one seat per event, see their bookings and cancel them.
- An event can never be oversold, even if many people book at the same time.
- Input validation with clear error messages.
- Swagger UI to try the API from the browser.

## How overselling is prevented

The simple way to book a seat is: read the number of available seats, check it is more than 0, then save the number minus 1.
The problem is that two requests can read the same number at the same time, and then both of them get the last seat.

In this project the check and the update are done by the database in a single statement:

```sql
UPDATE events
SET available_seats = available_seats - 1
WHERE id = ? AND available_seats > 0;
```

The database tells us how many rows were updated. If it is 1 the seat was taken and the booking is saved.
If it is 0 there was no seat left and the API returns `409 Conflict`.
The code for this is in `EventRepository.decrementAvailableSeats` and `BookingService.bookEvent`.

This is checked by `BookingConcurrencyIntegrationTest`. It creates an event with 50 seats and starts 100 threads
that all try to book at the same moment against a real PostgreSQL database. The test passes only if exactly
50 bookings succeed, 50 are rejected as sold out, and the event has 0 seats left.

## API endpoints

| Method | Endpoint | Who can call it | Description |
|--------|----------|-----------------|-------------|
| POST | `/api/auth/register` | Anyone | Create an account (role is `ORGANISER` or `ATTENDEE`) |
| POST | `/api/auth/login` | Anyone | Login and get a JWT |
| GET | `/api/events` | Anyone | List all events |
| GET | `/api/events/{id}` | Anyone | Get one event |
| GET | `/api/events/mine` | Organiser | Events created by me |
| POST | `/api/events` | Organiser | Create an event |
| PUT | `/api/events/{id}` | Organiser (owner only) | Update an event |
| DELETE | `/api/events/{id}` | Organiser (owner only) | Delete an event |
| POST | `/api/events/{eventId}/bookings` | Attendee | Book one seat |
| GET | `/api/bookings/mine` | Attendee | My bookings |
| POST | `/api/bookings/{bookingId}/cancel` | Attendee (owner only) | Cancel a booking |

For protected endpoints send the token in the header: `Authorization: Bearer <token>`

Full documentation is available in Swagger UI at `http://localhost:8080/swagger-ui.html` when the app is running.

### Example

```bash
# register as an organiser
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha","email":"asha@example.com","password":"secret123","role":"ORGANISER"}'

# create an event (use the token from the response above)
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"Tech Fest","description":"Annual fest","venue":"Main Hall","eventDate":"2027-05-01T18:00:00","totalSeats":50}'
```

## Running the project

### Option 1: Docker (easiest)

You only need Docker installed.

```bash
cp .env.example .env        # then edit .env and set your own password and secret
docker compose up -d --build
```

The API runs on `http://localhost:8080`.

### Option 2: Run locally

You need Java 17+, Maven and PostgreSQL.

1. Create the databases:
   ```sql
   CREATE DATABASE ticketdb;
   CREATE DATABASE ticketdb_test;
   ```
2. The app uses the user `postgres` with password `postgres` by default. To change this set the
   environment variables `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.
3. Start the app:
   ```bash
   mvn spring-boot:run
   ```

## Running the tests

```bash
mvn test
```

- Unit tests (JUnit 5 + Mockito) for the services and the JWT helper. These do not need a database.
- One integration test for concurrent bookings. It needs PostgreSQL running with the `ticketdb_test` database.

## CI pipeline

`.github/workflows/ci.yml` runs on every push. It starts a PostgreSQL container, runs all the tests and then builds the Docker image.

## Deploying on AWS EC2

These are the steps I used to deploy the API on a free tier EC2 instance.

1. Launch an EC2 instance (Ubuntu 24.04, `t2.micro` or `t3.micro`) and download the key pair.
2. In the security group allow inbound port `22` (SSH, only from your IP) and port `8080` (the API).
3. Connect to the instance:
   ```bash
   ssh -i my-key.pem ubuntu@<EC2_PUBLIC_IP>
   ```
4. Install Docker:
   ```bash
   sudo apt update
   sudo apt install -y docker.io docker-compose-v2 git
   sudo usermod -aG docker ubuntu
   ```
   Log out and log in again so the group change is applied.
5. Get the code and start it:
   ```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   cd <your-repo>
   cp .env.example .env
   nano .env                  # set a strong DB_PASSWORD and JWT_SECRET
   docker compose up -d --build
   ```
6. Open `http://<EC2_PUBLIC_IP>:8080/swagger-ui.html` in the browser.

To deploy a new version, run `git pull` and then `docker compose up -d --build` again.

Note: a `t2.micro` has only 1 GB of RAM and the Maven build inside Docker can run out of memory.
If that happens add 1 GB of swap first:

```bash
sudo fallocate -l 1G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
```

## Project structure

```
src/main/java/com/utkarsh/ticketbooking
├── config        Swagger configuration
├── controller    REST endpoints
├── dto           Request and response objects
├── exception     Custom exceptions and the global error handler
├── model         JPA entities (User, Event, Booking)
├── repository    Database access
├── security      JWT filter, JWT helper and security configuration
└── service       Business logic
```

## Known limitations

- The total seats of an event cannot be changed after it is created.
- Each booking is for one seat only.
- All dates and times are in UTC.
