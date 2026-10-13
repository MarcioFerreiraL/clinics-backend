package br.com.clinics.catalog.infrastructure.web;

import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetClinicProfileUseCase;
import br.com.clinics.catalog.infrastructure.web.ExamDtos.ClinicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinics")
@Tag(name = "Clínicas")
class ClinicController {

    private final GetClinicProfileUseCase profile;

    ClinicController(GetClinicProfileUseCase profile) {
        this.profile = profile;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público da clínica, horários e exames (UC05)")
    ClinicProfileResponse get(@PathVariable UUID id) {
        return ClinicProfileResponse.from(profile.getById(id));
    }
}
