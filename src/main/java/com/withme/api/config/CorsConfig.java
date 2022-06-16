package com.withme.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");   //허용할 ip
        config.addAllowedHeader("*");   //허용할 header
        config.addAllowedMethod("*");   //허용할 httpMehod(GET, POST, etc..)

        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }

}
