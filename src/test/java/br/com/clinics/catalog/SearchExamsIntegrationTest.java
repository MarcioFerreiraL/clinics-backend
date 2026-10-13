package br.com.clinics.catalog;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa os dados do seed (V5): "Ultrassom Abdominal Total" tem 5 ofertas, de R$ 90 a R$ 140, média R$ 118. */
@IntegrationTest
class SearchExamsIntegrationTest {

    // Praça central de Surubim, usada como posição do paciente
    private static final String LAT = "-7.8335";
    private static final String LNG = "-35.7530";

    @Autowired MockMvc mvc;

    @Test
    void searchIsPublicAndComparesPricesCheapestFirst() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "ultrassom abdominal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.data[0].clinic.name").value("Clínica Santa Maria"))
                .andExpect(jsonPath("$.data[0].price").value(90.00))
                .andExpect(jsonPath("$.data[0].average_price").value(118.00))
                .andExpect(jsonPath("$.data[0].savings").value(28.00))
                .andExpect(jsonPath("$.data[0].best_price").value(true))
                .andExpect(jsonPath("$.data[1].best_price").value(false))
                .andExpect(jsonPath("$.data[4].price").value(140.00))
                .andExpect(jsonPath("$.data[4].savings").value(0.00));
    }

    @Test
    void searchIgnoresCaseAndAccents() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "RAIO-X DE TORAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void filtersByCategory() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("category", "cardio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].exam_type.category").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Cardio"))));
    }

    @Test
    void unknownTermReturnsEmptyList() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "xyzinexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void likeWildcardsInTheQueryAreTreatedAsLiteralText() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void sortsByDistanceAndReportsKilometers() throws Exception {
        mvc.perform(get("/api/v1/exams/search")
                        .param("query", "ultrassom abdominal").param("sort_by", "distance")
                        .param("lat", LAT).param("lng", LNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].distance_km").isNumber())
                .andExpect(jsonPath("$.data[0].clinic.name").value("Clínica Santa Maria"));
    }

    @Test
    void distanceSortRequiresLocation() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("sort_by", "distance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOCATION_REQUIRED"));
    }

    @Test
    void rejectsIncompleteOrInvalidLocation() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("lat", LAT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOCATION_INCOMPLETE"));
        mvc.perform(get("/api/v1/exams/search").param("lat", "95").param("lng", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void rejectsInvalidCategoryAndSort() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("category", "Dentista"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
        mvc.perform(get("/api/v1/exams/search").param("sort_by", "cheapest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void paginatesAndCapsPageSize() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total_pages").value(16));
        mvc.perform(get("/api/v1/exams/search").param("size", "1000"))
                .andExpect(jsonPath("$.size").value(50));
    }
}
