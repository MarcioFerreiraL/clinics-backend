package br.com.clinics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class OpenApiIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void contractIsPublishedWithoutAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Clinics API"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"));
    }

    @Test
    void documentsEveryUseCaseEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/exams/search'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/exams/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/exams/{id}/contacts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/exam-types/trending'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/clinics/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/clinics/{clinicId}/reviews'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/clinics/{clinicId}/reviews/me'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/avatar'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/password'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/favorites'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/favorites/{examTypeId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/savings'].get").exists());
    }

    @Test
    void documentationUsesSnakeCaseLikeTheRealResponses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.full_name").exists())
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.fullName").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AuthResponse.properties.token_type").exists())
                .andExpect(jsonPath("$.components.schemas.ClinicProfileResponse.properties.is_verified").exists())
                .andExpect(jsonPath("$.components.schemas.ClinicProfileResponse.properties.exams_count").exists())
                .andExpect(jsonPath("$.components.schemas.FavoriteItem.properties.exam_type_id").exists())
                .andExpect(jsonPath("$.components.schemas.SavingsResponse.properties.total_savings").exists());
    }

    @Test
    void protectedEndpointsDeclareTheSecurityRequirement() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/me'].get.security[0]['bearer-jwt']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/exams/search'].get.security").doesNotExist());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void rootForwardsToTheDownloadPage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @Test
    void downloadPageLinksToTheApkThePwaAndTheDocs() throws Exception {
        String html = new String(new org.springframework.core.io.ClassPathResource("static/index.html")
                .getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(html)
                .contains("Baixar APK", "/download/clinics.apk", "href=\"/app/\"", "href=\"/docs\"",
                        "MárcioDev");
    }

    @Test
    void docsRedirectsToTheInteractiveDocumentation() throws Exception {
        mvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/swagger-ui/index.html")));
    }

    @Test
    void appPathRedirectsToTheTrailingSlashVersion() throws Exception {
        mvc.perform(get("/app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/app/")));
    }

    @Test
    void sitePublicAssetsDoNotRequireAuthentication() throws Exception {
        mvc.perform(get("/logo.png")).andExpect(status().isOk());
        mvc.perform(get("/favicon.png")).andExpect(status().isOk());
        mvc.perform(get("/qr-android.svg")).andExpect(status().isOk());
        mvc.perform(get("/qr-app.svg")).andExpect(status().isOk());
    }

    @Test
    void rootIsNotListedInTheContract() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(jsonPath("$.paths['/']").doesNotExist());
    }

    @Test
    void behindTheProxyTheSwaggerUsesTheForwardedHostAndProtocol() throws Exception {
        mvc.perform(get("/v3/api-docs").header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "clinics.marciodev.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://clinics.marciodev.com"));
    }
}
