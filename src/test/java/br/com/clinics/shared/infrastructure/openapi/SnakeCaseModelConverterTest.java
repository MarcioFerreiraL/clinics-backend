package br.com.clinics.shared.infrastructure.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnakeCaseModelConverterTest {

    @Test
    void convertsCamelCaseLikeJacksonSnakeCase() {
        assertThat(SnakeCaseModelConverter.toSnakeCase("fullName")).isEqualTo("full_name");
        assertThat(SnakeCaseModelConverter.toSnakeCase("isVerified")).isEqualTo("is_verified");
        assertThat(SnakeCaseModelConverter.toSnakeCase("examTypeId")).isEqualTo("exam_type_id");
        assertThat(SnakeCaseModelConverter.toSnakeCase("distanceKm")).isEqualTo("distance_km");
    }

    @Test
    void leavesAlreadyConvertedAndSimpleNamesUntouched() {
        assertThat(SnakeCaseModelConverter.toSnakeCase("full_name")).isEqualTo("full_name");
        assertThat(SnakeCaseModelConverter.toSnakeCase("price")).isEqualTo("price");
        assertThat(SnakeCaseModelConverter.toSnakeCase("id")).isEqualTo("id");
    }
}
