package com.example.WebShopSEP;

import com.example.WebShopSEP.service.PSPClientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan // Ensures your ConfigProperties class is picked up
public class WebShopSepApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebShopSepApplication.class, args);
	}

	@Bean
	public CommandLineRunner testHandshake(PSPClientService PSPClientService) {
		return args -> {
			System.out.println("--- TESTING HANDSHAKE ON STARTUP ---");
			try {
				PSPClientService.connectToMerchantBackend();
				System.out.println("Handshake test execution finished.");
			} catch (Exception e) {
				System.err.println("Handshake failed during startup: " + e.getMessage());
			}
			System.out.println("------------------------------------");
		};
	}
}