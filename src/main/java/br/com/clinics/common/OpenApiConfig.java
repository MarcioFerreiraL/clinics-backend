package br.com.clinics.common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato da API em {@code /v3/api-docs} e navegável em {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI clinicsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinics API")
                        .version("v1")
                        .description("""
                                Comparação de preços de exames e consultas em Surubim-PE.

                                **Autenticação:** faça login em `POST /api/v1/auth/login`, copie o `token` e use o botão \
                                *Authorize*. Rotas de busca e detalhes são públicas.

                                **Erros:** seguem a RFC 9457 (`application/problem+json`) com um campo `code` estável.""")
                        .contact(new Contact().name("Márcio Ferreira Lima e Lucas Nascimento Barros")))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /**
     * O JSON da API é {@code snake_case}, mas o springdoc gera os schemas em camelCase.
     * Este conversor renomeia só as propriedades dos schemas, sem mexer no resto do documento.
     */
    @Bean
    ModelConverter snakeCaseSchemas() {
        return new ModelConverter() {
            @Override
            @SuppressWarnings("rawtypes")
            public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
                if (!chain.hasNext()) {
                    return null;
                }
                Schema schema = chain.next().resolve(type, context, chain);
                if (schema != null) {
                    rename(schema);
                }
                context.getDefinedModels().values().forEach(OpenApiConfig::rename);
                return schema;
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void rename(Schema schema) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            Map<String, Schema> renamed = new LinkedHashMap<>();
            properties.forEach((name, value) -> renamed.put(ErrorHandler.snakeCase(name), value));
            schema.setProperties(renamed);
        }
        List<String> required = schema.getRequired();
        if (required != null) {
            schema.setRequired(required.stream().map(ErrorHandler::snakeCase).toList());
        }
    }
}
