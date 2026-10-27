package br.com.clinics.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Site público: a raiz serve a página de download ({@code static/index.html}), {@code /app/} o app web (PWA),
 * {@code /docs} a documentação, {@code /files/avatars/} as fotos de perfil e {@code /download/clinics.apk} o APK.
 */
@Configuration
@RestController
@Hidden
public class WebConfig implements WebMvcConfigurer {

    public static final String AVATARS_PREFIX = "/files/avatars/";
    private static final MediaType APK = MediaType.parseMediaType("application/vnd.android.package-archive");

    private final Path avatarsDir;
    private final Path downloadsDir;

    public WebConfig(@Value("${app.storage.avatars-dir:./data/avatars}") Path avatarsDir,
            @Value("${app.downloads-dir:./data/downloads}") Path downloadsDir) {
        this.avatarsDir = avatarsDir.toAbsolutePath().normalize();
        this.downloadsDir = downloadsDir.toAbsolutePath().normalize();
    }

    /** Fuso do negócio: "aberta agora" e "nascimento no futuro" usam o horário de Recife. */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Recife"));
    }

    @Bean
    Path avatarsDir() {
        return avatarsDir;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/docs", "/swagger-ui/index.html");
        registry.addRedirectViewController("/app", "/app/");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(AVATARS_PREFIX + "**").addResourceLocations(avatarsDir.toUri().toString());
    }

    @GetMapping("/download/clinics.apk")
    ResponseEntity<Resource> apk() throws IOException {
        Path file = downloadsDir.resolve("clinics.apk");
        if (!Files.isRegularFile(file)) {
            throw ApiException.notFound("APK_NOT_AVAILABLE", "O aplicativo Android ainda não está disponível para download.");
        }
        return ResponseEntity.ok()
                .contentType(APK)
                .contentLength(Files.size(file))
                .lastModified(Instant.ofEpochMilli(Files.getLastModifiedTime(file).toMillis()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("clinics.apk").build().toString())
                .body(new FileSystemResource(file));
    }
}
