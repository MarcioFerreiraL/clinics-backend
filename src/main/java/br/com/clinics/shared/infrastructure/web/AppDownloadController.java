package br.com.clinics.shared.infrastructure.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import br.com.clinics.shared.domain.NotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega o APK do Android direto pelo site (sem mandar o usuário para o repositório).
 * O arquivo fica em {@code app.downloads-dir} (volume do servidor) e é atualizado pelo deploy.
 */
@RestController
@Hidden
class AppDownloadController {

    static final String APK_NAME = "clinics.apk";
    private static final MediaType APK = MediaType.parseMediaType("application/vnd.android.package-archive");

    private final Path downloadsDir;

    AppDownloadController(@Value("${app.downloads-dir:./data/downloads}") Path downloadsDir) {
        this.downloadsDir = downloadsDir.toAbsolutePath().normalize();
    }

    @GetMapping("/download/" + APK_NAME)
    ResponseEntity<Resource> apk() throws IOException {
        Path file = downloadsDir.resolve(APK_NAME);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("APK_NOT_AVAILABLE", "O aplicativo Android ainda não está disponível para download.");
        }
        return ResponseEntity.ok()
                .contentType(APK)
                .contentLength(Files.size(file))
                .lastModified(Instant.ofEpochMilli(Files.getLastModifiedTime(file).toMillis()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(APK_NAME).build().toString())
                .body(new FileSystemResource(file));
    }
}
