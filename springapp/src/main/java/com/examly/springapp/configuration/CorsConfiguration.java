package com.examly.springapp.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
        .allowedOrigins("https://8081-fbeecedcbabbdbbdfafcfffbdacddbfcdceaaffcc.premiumproject.examly.io", "http://localhost:3000", "http://localhost:5173") 
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);

    }
}
// https://8081-ddaafeadaccddcacbeacdbbfecbffbbdeeecceea.premiumproject.examly.io/

