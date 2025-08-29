package com.example.wholesalesalesbackend;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@SpringBootApplication(scanBasePackages = { "com.example.wholesalesalesbackend", "com.service" })
@EnableScheduling
public class WholesaleSalesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WholesaleSalesBackendApplication.class, args);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer customizer() {
		return builder -> builder
				.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME))
				.deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
	}

	// minutes
	@Scheduled(cron = "0 */1 * * * *", zone = "Asia/Kolkata")
	public void printHello() {
		System.out.println("Hello");
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
