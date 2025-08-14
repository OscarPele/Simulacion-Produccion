package com.oscar.proyecto.ms_hr;

import com.oscar.shared.security.MSSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(MSSecurityConfig.class)
public class MsHrApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsHrApplication.class, args);
	}

}
