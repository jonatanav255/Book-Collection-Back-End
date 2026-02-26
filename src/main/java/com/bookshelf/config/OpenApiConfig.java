package com.bookshelf.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI bookshelfOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookShelf API")
                        .description("REST API for BookShelf — manage your book library, reading progress, notes, preferences, and audio narration.")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local dev server")));
    }
}
