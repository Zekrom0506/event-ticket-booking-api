package com.utkarsh.ticketbooking.service;

import com.utkarsh.ticketbooking.dto.AuthResponse;
import com.utkarsh.ticketbooking.dto.LoginRequest;
import com.utkarsh.ticketbooking.dto.RegisterRequest;
import com.utkarsh.ticketbooking.exception.BadRequestException;
import com.utkarsh.ticketbooking.model.Role;
import com.utkarsh.ticketbooking.model.User;
import com.utkarsh.ticketbooking.repository.UserRepository;
import com.utkarsh.ticketbooking.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Asha");
        request.setEmail("asha@test.com");
        request.setPassword("plain-password");
        request.setRole(Role.ATTENDEE);

        when(userRepository.existsByEmail("asha@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(jwtUtil.generateToken("asha@test.com", "ATTENDEE")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        // capture the user that was saved and check the password is the hash, not the plain text
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashed-password", userCaptor.getValue().getPassword());

        assertEquals("jwt-token", response.getToken());
        assertEquals("ATTENDEE", response.getRole());
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Asha");
        request.setEmail("asha@test.com");
        request.setPassword("plain-password");
        request.setRole(Role.ATTENDEE);

        when(userRepository.existsByEmail("asha@test.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenForCorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@test.com");
        request.setPassword("plain-password");

        User user = new User("Asha", "asha@test.com", "hashed-password", Role.ORGANISER);
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("asha@test.com", "ORGANISER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("asha@test.com", response.getEmail());
    }

    @Test
    void loginFailsForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@test.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
