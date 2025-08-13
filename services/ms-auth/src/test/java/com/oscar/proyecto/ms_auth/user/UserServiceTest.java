package com.oscar.proyecto.ms_auth.user;

import com.oscar.proyecto.ms_auth.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repo, encoder);
    }

    // -------- register --------

    @Test
    void register_ok_hashes_password_and_saves() {
        when(repo.existsByUsername("alice")).thenReturn(false);
        when(repo.existsByEmail("alice@mail.com")).thenReturn(false);
        when(encoder.encode("Secret123")).thenReturn("HASHED");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register("alice", "alice@mail.com", "Secret123");

        assertNotNull(saved);
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repo).save(cap.capture());
        assertEquals("alice", cap.getValue().getUsername());
        assertEquals("alice@mail.com", cap.getValue().getEmail());
        assertEquals("HASHED", cap.getValue().getPasswordHash());
    }

    @Test
    void register_fails_when_username_exists() {
        when(repo.existsByUsername("bob")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> service.register("bob", "b@mail.com", "Secret123"));

        verify(repo, never()).save(any());
    }

    @Test
    void register_fails_when_email_exists() {
        when(repo.existsByUsername("charlie")).thenReturn(false);
        when(repo.existsByEmail("charlie@mail.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register("charlie", "charlie@mail.com", "Secret123"));

        verify(repo, never()).save(any());
    }

    // -------- authenticate --------

    @Test
    void authenticate_ok_by_username() {
        User db = new User();
        db.setId(1L);
        db.setUsername("dana");
        db.setEmail("d@x.com");
        db.setPasswordHash("HASHED");

        when(repo.findByUsername("dana")).thenReturn(Optional.of(db));
        when(repo.findByEmail("dana")).thenReturn(Optional.empty());
        when(encoder.matches("Secret123", "HASHED")).thenReturn(true);

        User out = service.authenticate("dana", "Secret123");

        assertEquals(1L, out.getId());
        assertEquals("dana", out.getUsername());
    }

    @Test
    void authenticate_ok_by_email() {
        User db = new User();
        db.setEmail("e@x.com");
        db.setPasswordHash("HASHED");

        when(repo.findByUsername("e@x.com")).thenReturn(Optional.empty());
        when(repo.findByEmail("e@x.com")).thenReturn(Optional.of(db));
        when(encoder.matches("Secret123", "HASHED")).thenReturn(true);

        User out = service.authenticate("e@x.com", "Secret123");
        assertEquals("e@x.com", out.getEmail());
    }

    @Test
    void authenticate_fails_on_bad_password() {
        User db = new User();
        db.setUsername("fran");
        db.setPasswordHash("HASHED");

        when(repo.findByUsername("fran")).thenReturn(Optional.of(db));
        when(repo.findByEmail("fran")).thenReturn(Optional.empty());
        when(encoder.matches("bad", "HASHED")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate("fran", "bad"));
    }

    @Test
    void authenticate_fails_when_user_not_found() {
        when(repo.findByUsername("ghost")).thenReturn(Optional.empty());
        when(repo.findByEmail("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate("ghost", "whatever"));
    }

    // -------- requireById --------

    @Test
    void requireById_throws_404_when_missing() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> service.requireById(999L));
    }

    // -------- changePassword --------

    @Test
    void changePassword_ok_updates_hash() {
        User db = new User();
        db.setId(10L);
        db.setPasswordHash("OLD");

        when(repo.findById(10L)).thenReturn(Optional.of(db));
        when(encoder.matches("old123", "OLD")).thenReturn(true);
        when(encoder.encode("new456")).thenReturn("NEW");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changePassword(10L, "old123", "new456");

        assertEquals("NEW", db.getPasswordHash());
        verify(repo).save(db);
    }

    @Test
    void changePassword_fails_when_current_wrong() {
        User db = new User();
        db.setId(11L);
        db.setPasswordHash("OLD");

        when(repo.findById(11L)).thenReturn(Optional.of(db));
        when(encoder.matches("bad", "OLD")).thenReturn(false);

        assertThrows(CurrentPasswordIncorrectException.class,
                () -> service.changePassword(11L, "bad", "new456"));

        verify(repo, never()).save(any());
    }
}
