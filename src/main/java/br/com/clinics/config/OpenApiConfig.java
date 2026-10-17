package br.com.clinics.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato da API publicado em {@code /v3/api-docs} e navegável em {@code /swagger-ui.html}. */
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
                                *Authorize* com o valor recebido. Rotas de busca e detalhes são públicas.

                                **Erros:** todas as falhas seguem a RFC 9457 (`application/problem+json`) com um campo \
                                `code` estável para tratamento no app.""")
                        .contact(new Contact().name("Márcio Ferreira Lima e Lucas Nascimento Barros")))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtido no login ou no cadastro.")))
                .tags(List.of(
                        new Tag().name("Autenticação").description("UC01 e UC02"),
                        new Tag().name("Exames").description("UC03, UC04 e UC06: busca, comparação, detalhes e exames em alta"),
                        new Tag().name("Clínicas").description("UC05: perfil público da clínica"),
                        new Tag().name("Contato").description("UC07 e UC08: WhatsApp e economia"),
                        new Tag().name("Favoritos").description("UC09"),
                        new Tag().name("Avaliações").description("UC10"),
                        new Tag().name("Perfil").description("UC11 e UC12")));
    }
}
