package com.ufc.web.chatly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class ChatLyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatLyApplication.class, args);
		System.out.println("=======================");
		System.out.println("\tServer ON");
		System.out.println("=======================");
	}

	@SuppressWarnings("deprecation")
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("**").allowedOrigins("**");
			}
		};
	}
}
