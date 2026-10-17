package br.com.clinics.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Path avatarsDir) {

    public StorageProperties {
        if (avatarsDir == null) {
            avatarsDir = Path.of("data", "avatars");
        }
        avatarsDir = avatarsDir.toAbsolutePath().normalize();
    }
}
