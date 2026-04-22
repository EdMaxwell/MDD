package com.mdd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the MDD Spring Boot application.
 */
@SpringBootApplication
public class MddApplication {

	/**
	 * Application entry point.
	 *
	 * @param args command-line arguments forwarded to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(MddApplication.class, args);
	}

}
