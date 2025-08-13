package com.oscar.proyecto.ms_auth.user;

import com.oscar.proyecto.ms_auth.exception.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable("id") Long id,
                                               @Valid @RequestBody ChangePasswordRequest body) {
        // Usuario objetivo (por ID de la ruta)
        var target = users.requireById(id);

        // Usuario autenticado (establecido por  filtro JWT: principal = username)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        // Solo el dueño del recurso
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
