package org.dcsa;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories
public class Application {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info().title("Disposal Collection Care API").description(
						"This is a sample Spring Boot RESTful service using springdoc-openapi and OpenAPI 3.").version("1.0"));
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
