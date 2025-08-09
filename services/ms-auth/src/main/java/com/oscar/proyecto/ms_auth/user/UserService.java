package com.oscar.proyecto.ms_auth.user;

import com.oscar.proyecto.ms_auth.exception.EmailAlreadyExistsException;
import com.oscar.proyecto.ms_auth.exception.UsernameAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.userRepo = repo; this.encoder = encoder;
    }

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepo.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        return userRepo.save(u);
    }
}
