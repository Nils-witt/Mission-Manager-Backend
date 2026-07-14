package dev.nilswitt.mission_manager.api;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT obtained from POST /api/token. Send it as 'Authorization: Bearer <token>'."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI missionManagerOpenApi(@Value("${spring.application.version}") String version) {
        return new OpenAPI()
            .info(
                new Info()
                    .title("Mission Manager API")
                    .description("REST API for obtaining and validating authentication tokens.")
                    .version(version)
            );
    }
}
