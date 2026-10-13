package br.com.clinics.shared.infrastructure.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldNamesTest {

    @Test
    void convertsCamelCaseLikeJacksonSnakeCase() {
        assertThat(FieldNames.toSnakeCase("fullName")).isEqualTo("full_name");
        assertThat(FieldNames.toSnakeCase("isVerified")).isEqualTo("is_verified");
        assertThat(FieldNames.toSnakeCase("examTypeId")).isEqualTo("exam_type_id");
        assertThat(FieldNames.toSnakeCase("distanceKm")).isEqualTo("distance_km");
    }

    @Test
    void leavesAlreadyConvertedAndSimpleNamesUntouched() {
        assertThat(FieldNames.toSnakeCase("full_name")).isEqualTo("full_name");
        assertThat(FieldNames.toSnakeCase("price")).isEqualTo("price");
        assertThat(FieldNames.toSnakeCase("id")).isEqualTo("id");
    }
}
