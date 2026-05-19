package com.dentruth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DentruthApplication {

	public static void main(String[] args) {
		SpringApplication.run(DentruthApplication.class, args);
	}

}
