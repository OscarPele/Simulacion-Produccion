package com.oscar.proyecto.ms_auth.user;

import com.oscar.proyecto.ms_auth.exception.ForbiddenOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @Operation(
            summary = "Cambiar contraseña",
            description = """
                    Permite a un usuario autenticado cambiar su propia contraseña.
                    Validaciones:
                    - **currentPassword**: no vacío.
                    - **newPassword**: no vacío.
                    Solo el propietario de la cuenta puede realizar esta operación.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ChangePasswordRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "currentPassword": "OldPass123",
                                              "newPassword": "NewStrongPass456"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contraseña cambiada correctamente"),
                    @ApiResponse(responseCode = "403", description = "Intento de cambio de contraseña a otro usuario")
            }
    )
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable("id") Long id,
                                               @Valid @RequestBody ChangePasswordRequest body) {
        var target = users.requireById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (!target.getUsername().equals(currentUsername)) {
            throw new ForbiddenOperationException();
        }

        users.changePassword(id, body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {}
}
