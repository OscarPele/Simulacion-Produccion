package com.oscar.proyecto.ms_auth.api;

import com.oscar.proyecto.ms_auth.api.dto.*;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.token.RefreshToken;
import com.oscar.proyecto.ms_auth.token.RefreshTokenService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.register(request.username(), request.email(), request.password());
        return ResponseEntity.ok(new UserResponse(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail(),
                newUser.isEnabled(),
                newUser.getCreatedAt()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = userService.authenticate(request.usernameOrEmail(), request.password());

        String accessToken = jwtService.generate(
                user.getUsername(),
                Map.of("uid", user.getId())
        );

        RefreshToken rt = refreshTokenService.create(user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                accessToken,
                jwtService.getExpirationSeconds(),
                rt.getToken(),
                refreshTokenService.getRefreshExpirationSeconds()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            var result = refreshTokenService.rotate(request.refreshToken());

            String newAccessToken = jwtService.generate(
                    result.user().username(),
                    Map.of("uid", result.user().id())
            );

            return ResponseEntity.ok(new TokenResponse(
                    "Bearer",
                    newAccessToken,
                    jwtService.getExpirationSeconds(),
                    result.newRefresh().getToken(),
                    refreshTokenService.getRefreshExpirationSeconds()
            ));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revoke(request.refreshToken());
        } catch (Exception ignored) { /* no filtramos si el token no existe */ }
        return ResponseEntity.noContent().build(); // 204
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            var user = refreshTokenService.validateAndGetUserRef(request.refreshToken());
            refreshTokenService.revokeAllByUserId(user.id());
        } catch (Exception ignored) { /* idem: no filtramos info */ }
        return ResponseEntity.noContent().build(); // 204
    }
}
