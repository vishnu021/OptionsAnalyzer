package com.vish.fno.manage;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "Options Analyzer",
                description = "Project to analyze the technicals of stock options available in National Stock Exchange - India",
                contact = @Contact(
                        name = "Vishnu Shankar",
                        email = "vish045@gmail.com"
                )
        )
)
@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass")
public class OrderManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagerApplication.class, args);
    }
}