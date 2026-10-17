package br.com.clinics.config;

import br.com.clinics.storage.LocalAvatarStorage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve os avatares gravados em disco em {@code /files/avatars/**}. */
@Configuration
public class StaticFilesConfig implements WebMvcConfigurer {

    private final StorageProperties properties;

    public StaticFilesConfig(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(LocalAvatarStorage.PUBLIC_PREFIX + "**")
                .addResourceLocations(properties.avatarsDir().toUri().toString());
    }
}
