package org.cherrypic.global.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final SpringEnvironmentHelper springEnvironmentHelper;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("CherryPic Server API")
                                .description("CherryPic Server API 명세서입니다.")
                                .version("v0.0.1"))
                .servers(initializeSwaggerServers());
    }

    private List<Server> initializeSwaggerServers() {
        return List.of(new Server().url(resolveServerUrlByProfile()));
    }

    private String resolveServerUrlByProfile() {
        return switch (springEnvironmentHelper.getCurrentProfile()) {
            case "prod" -> "https://api.cherrypic.today";
            case "dev" -> "https://dev-api.cherrypic.today";
            default -> "http://localhost:8080";
        };
    }
}
