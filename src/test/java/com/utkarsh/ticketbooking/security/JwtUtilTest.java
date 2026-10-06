package com.utkarsh.ticketbooking.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3600000);

    @Test
    void generatedTokenContainsEmail() {
        String token = jwtUtil.generateToken("user@test.com", "ATTENDEE");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("user@test.com", jwtUtil.extractEmail(token));
    }

    @Test
    void randomStringIsNotAValidToken() {
        assertFalse(jwtUtil.isTokenValid("this.is.not-a-token"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil otherJwtUtil = new JwtUtil("a-completely-different-secret-key-for-testing-1234", 3600000);
        String token = otherJwtUtil.generateToken("user@test.com", "ATTENDEE");

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void expiredTokenIsRejected() {
        // negative expiry means the token is already expired when it is created
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000);
        String token = expiredJwtUtil.generateToken("user@test.com", "ATTENDEE");

        assertFalse(jwtUtil.isTokenValid(token));
    }
}
