package br.com.clinics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O botão "Baixar APK" do site baixa o arquivo do próprio servidor, sem passar pelo repositório. */
@IntegrationTest
class AppDownloadIntegrationTest {

    private static final Path DIR = createDir();
    private static final byte[] FAKE_APK = "PK-conteudo-de-teste-do-apk".getBytes();

    @DynamicPropertySource
    static void downloads(DynamicPropertyRegistry registry) {
        registry.add("app.downloads-dir", DIR::toString);
    }

    private static Path createDir() {
        try {
            return Files.createTempDirectory("clinics-downloads");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired MockMvc mvc;

    @AfterEach
    void cleanUp() throws IOException {
        Files.deleteIfExists(DIR.resolve("clinics.apk"));
    }

    @Test
    @DisplayName("Download · sem o arquivo publicado, responde 404 padronizado")
    void returnsNotFoundWhenTheApkIsNotPublished() throws Exception {
        mvc.perform(get("/download/clinics.apk"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("APK_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("Download · entrega o APK como anexo, sem exigir login")
    void servesTheApkAsAnAttachment() throws Exception {
        Files.write(DIR.resolve("clinics.apk"), FAKE_APK);

        mvc.perform(get("/download/clinics.apk"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.android.package-archive"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("clinics.apk")))
                .andExpect(header().longValue("Content-Length", FAKE_APK.length))
                .andExpect(content().bytes(FAKE_APK));
    }

    @Test
    @DisplayName("Download · aceita retomar o download por faixa de bytes (celular com sinal fraco)")
    void supportsRangeRequests() throws Exception {
        Files.write(DIR.resolve("clinics.apk"), FAKE_APK);

        mvc.perform(get("/download/clinics.apk").header("Range", "bytes=0-1"))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes(new byte[] {FAKE_APK[0], FAKE_APK[1]}));
    }

    @Test
    @DisplayName("Download · só existe o arquivo do APK (não lista nem expõe outros arquivos)")
    void doesNotExposeOtherFiles() throws Exception {
        Files.write(DIR.resolve("segredo.txt"), "x".getBytes());
        try {
            mvc.perform(get("/download/segredo.txt")).andExpect(status().is4xxClientError());
        } finally {
            Files.deleteIfExists(DIR.resolve("segredo.txt"));
        }
    }
}
