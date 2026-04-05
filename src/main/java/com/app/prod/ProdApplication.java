package com.app.prod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProdApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProdApplication.class, args);
	}

}
