package com.oscar.proyecto.ms_auth.user;

import com.oscar.proyecto.ms_auth.exception.CurrentPasswordIncorrectException;
import com.oscar.proyecto.ms_auth.exception.EmailAlreadyExistsException;
import com.oscar.proyecto.ms_auth.exception.InvalidCredentialsException;
import com.oscar.proyecto.ms_auth.exception.UsernameAlreadyExistsException;
import com.oscar.proyecto.ms_auth.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.userRepo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepo.existsByUsername(username)) throw new UsernameAlreadyExistsException();
        if (userRepo.existsByEmail(email)) throw new EmailAlreadyExistsException();

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        return userRepo.save(u);
    }

    @Transactional(readOnly = true)
    public User authenticate(String usernameOrEmail, String rawPassword) {
        var user = userRepo.findByUsername(usernameOrEmail)
                .or(() -> userRepo.findByEmail(usernameOrEmail))
                .orElseThrow(InvalidCredentialsException::new);

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireById(Long id) {
        return userRepo.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        var user = requireById(id);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);
    }
}
