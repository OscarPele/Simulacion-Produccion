package com.oscar.proyecto.ms_auth.User;

import com.oscar.proyecto.ms_auth.user.UserRepository;
import com.oscar.proyecto.ms_auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository repo;

    @Test
    void register_crea_usuario_y_hashea_password() {
        var u = userService.register("oscar_test", "oscar_test@example.com", "MiPassw0rd!");
        assertThat(u.getId()).isNotNull();
        assertThat(u.getPasswordHash()).isNotEqualTo("MiPassw0rd!");
        assertThat(repo.existsByUsername("oscar_test")).isTrue();
    }
}
