package com.oscar.proyecto.ms_auth.api;

import com.oscar.proyecto.ms_auth.api.dto.*;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.token.RefreshTokenService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import com.oscar.proyecto.ms_auth.password.PasswordResetService;
import com.oscar.proyecto.ms_auth.verification.EmailVerificationService;
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
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(
            summary = "Registrar un nuevo usuario",
            description = """
                    Crea un nuevo usuario con validación de campos:
                    - **username**: mínimo 3, máximo 50 caracteres, no vacío.
                    - **email**: formato válido, máximo 120 caracteres.
                    - **password**: mínimo 8, máximo 128 caracteres.
                    Tras registrar, se envía un email de verificación y la cuenta queda deshabilitada hasta confirmar.
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
                            description = "Usuario registrado correctamente (email de verificación enviado)",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario/email ya existente")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.register(request.username(), request.email(), request.password());
        emailVerificationService.send(newUser);
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

        RefreshTokenService.IssuedRefresh issued = refreshTokenService.create(user);

        return ResponseEntity.ok(new TokenResponse(
                "Bearer",
                accessToken,
                jwtService.getExpirationSeconds(),
                issued.plain(),
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
                                              "refreshToken": "c29tZS1iYXNlNjR1cmwtcmFuZG9tLXRva2Vu..."
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
                    result.newRefresh().plain(),
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
                                              "refreshToken": "c29tZS1iYXNlNjR1cmwtcmFuZG9tLXRva2Vu..."
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
        } catch (Exception ignored) { }
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
                                              "refreshToken": "c29tZS1iYXNlNjR1cmwtcmFuZG9tLXRva2Vu..."
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
            var userRef = refreshTokenService.validateAndGetUserRef(request.refreshToken());
            refreshTokenService.revokeAllByUserId(userRef.id());
        } catch (Exception ignored) { }
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Solicitar restablecimiento de contraseña",
            description = """
                    Envía un email con un enlace para restablecer la contraseña si el correo existe.
                    Por privacidad, este endpoint **siempre** responde 204.
                    Campo:
                    - **email**: formato válido.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ForgotPasswordRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            { "email": "johndoe@example.com" }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Si existe, se envían instrucciones por email")
            }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Restablecer contraseña con token",
            description = """
                    Cambia la contraseña usando un token de un solo uso recibido por email.
                    Campos:
                    - **token**: token de restablecimiento.
                    - **newPassword**: nueva contraseña (min 8).
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ResetPasswordRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "token": "base64url-token-aqui",
                                              "newPassword": "NewStrongPass456"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contraseña restablecida y sesiones revocadas"),
                    @ApiResponse(responseCode = "400", description = "Token inválido o expirado")
            }
    )
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.reset(request.token(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }
}
