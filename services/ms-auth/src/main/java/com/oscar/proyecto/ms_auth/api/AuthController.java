package com.oscar.proyecto.ms_auth.api;

import com.oscar.proyecto.ms_auth.api.dto.LoginRequest;
import com.oscar.proyecto.ms_auth.api.dto.LoginResponse;
import com.oscar.proyecto.ms_auth.api.dto.RegisterRequest;
import com.oscar.proyecto.ms_auth.api.dto.UserResponse;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new UserResponse(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail(),
                newUser.isEnabled(),
                newUser.getCreatedAt()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = userService.authenticate(request.getUsernameOrEmail(), request.getPassword());

        String token = jwtService.generate(
                user.getUsername(),
                Map.of("uid", user.getId(), "email", user.getEmail())
        );

        return ResponseEntity.ok(new LoginResponse(token, jwtService.getExpirationSeconds()));
    }
}
