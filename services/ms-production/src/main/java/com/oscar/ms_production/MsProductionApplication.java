package com.oscar.ms_production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.oscar.shared.security.MSSecurityConfig;

@SpringBootApplication
@Import(MSSecurityConfig.class)
public class MsProductionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsProductionApplication.class, args);
	}

}
