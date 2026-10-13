package br.com.clinics.identity.infrastructure.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import br.com.clinics.identity.application.port.out.AvatarStorage;
import br.com.clinics.identity.domain.AvatarImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Armazenamento em disco local. Trocar por S3 afeta apenas este adaptador. */
@Component
@EnableConfigurationProperties(StorageProperties.class)
class LocalAvatarStorage implements AvatarStorage {

    static final String PUBLIC_PREFIX = "/files/avatars/";
    private static final Logger log = LoggerFactory.getLogger(LocalAvatarStorage.class);

    private final Path directory;

    LocalAvatarStorage(StorageProperties properties) {
        this.directory = properties.avatarsDir();
    }

    @Override
    public String save(UUID userId, AvatarImage image) {
        String filename = "%s-%s.%s".formatted(userId, UUID.randomUUID().toString().substring(0, 8), image.extension());
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(filename), image.bytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o avatar", e);
        }
        return PUBLIC_PREFIX + filename;
    }

    @Override
    public void delete(String publicPath) {
        if (publicPath == null || !publicPath.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        Path target = directory.resolve(publicPath.substring(PUBLIC_PREFIX.length())).normalize();
        if (!target.startsWith(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Não foi possível remover o avatar antigo {}", target, e);
        }
    }
}
