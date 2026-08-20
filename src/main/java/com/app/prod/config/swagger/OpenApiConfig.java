package com.app.prod.config.swagger;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.web.BuildingScopeArgumentResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OperationCustomizer buildingScopeParameter() {
        return (operation, handlerMethod) -> {
            boolean buildingScoped = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> BuildingScope.class.equals(parameter.getParameterType()));

            if (buildingScoped) {
                operation.addParametersItem(new Parameter()
                        .in("query")
                        .name(BuildingScopeArgumentResolver.BUILDING_ID_PARAM)
                        .required(true)
                        .description("Building the operation is performed in")
                        .schema(new StringSchema().format("uuid")));
            }

            return operation;
        };
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Unite's API").version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}