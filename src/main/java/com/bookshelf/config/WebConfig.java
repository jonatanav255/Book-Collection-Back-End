package com.bookshelf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * CORS is handled by CorsFilterConfig (servlet filter level) so it runs
 * before Spring Security and all responses include CORS headers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
