package com.oscar.proyecto.ms_auth.api;

import com.oscar.proyecto.ms_auth.api.dto.*;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.token.RefreshToken;
import com.oscar.proyecto.ms_auth.token.RefreshTokenService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(
            summary = "Registrar un nuevo usuario",
            description = """
                    Crea un nuevo usuario con validación de campos:
                    - **username**: mínimo 3, máximo 50 caracteres, no vacío.
                    - **email**: formato válido, máximo 120 caracteres.
                    - **password**: mínimo 8, máximo 128 caracteres.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "username": "johndoe",
                                              "email": "johndoe@example.com",
                                              "password": "StrongPass123"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuario registrado correctamente",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario/email ya existente")
            }
    )
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

    @Operation(
            summary = "Iniciar sesión",
            description = """
                    Autentica al usuario usando **username** o **email** y contraseña.
                    Campos:
                    - **usernameOrEmail**: no vacío.
                    - **password**: no vacío.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "usernameOrEmail": "johndoe",
                                              "password": "StrongPass123"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login exitoso",
                            content = @Content(schema = @Schema(implementation = TokenResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
            }
    )
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

    @Operation(
            summary = "Renovar tokens",
            description = """
                    Recibe un refresh token válido y devuelve un nuevo access token y refresh token.
                    Campo:
                    - **refreshToken**: no vacío.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens renovados correctamente"),
                    @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
            }
    )
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

    @Operation(
            summary = "Cerrar sesión en un dispositivo",
            description = "Revoca el refresh token actual sin afectar otras sesiones.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
                                            }
                                            """
                            )
                    )
            ),
            responses = @ApiResponse(responseCode = "204", description = "Sesión cerrada correctamente")
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revoke(request.refreshToken());
        } catch (Exception ignored) { /* No filtramos si el token no existe */ }
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cerrar sesión en todos los dispositivos",
            description = "Revoca todas las sesiones activas del usuario autenticado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
                                            }
                                            """
                            )
                    )
            ),
            responses = @ApiResponse(responseCode = "204", description = "Todas las sesiones cerradas correctamente")
    )
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            var user = refreshTokenService.validateAndGetUserRef(request.refreshToken());
            refreshTokenService.revokeAllByUserId(user.id());
        } catch (Exception ignored) { /* Idem: no filtramos info */ }
        return ResponseEntity.noContent().build();
    }
}
