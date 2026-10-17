package br.com.clinics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rotas do site público: a raiz serve a página de download ({@code static/index.html}),
 * {@code /app/} serve o app web (PWA) e {@code /docs} leva à documentação da API.
 */
@Configuration
public class SiteConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/docs", "/swagger-ui/index.html");
        registry.addRedirectViewController("/app", "/app/");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
