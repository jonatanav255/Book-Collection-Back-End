package com.bookshelf.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

// Configures file upload limits for PDF book uploads (overrides default ~1MB)
@Configuration
public class MultipartConfig {

    // Registers this multipart config as a Spring-managed bean so it's used app-wide
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // Max size per individual file
        factory.setMaxFileSize(DataSize.ofMegabytes(100));
        // Max size for the entire request (must be >= max file size)
        factory.setMaxRequestSize(DataSize.ofMegabytes(100));
        return factory.createMultipartConfig();
    }
}
