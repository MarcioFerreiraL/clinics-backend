package br.com.clinics.controller;

import java.util.UUID;

import br.com.clinics.service.GetClinicProfileService;
import br.com.clinics.dto.ExamDtos.ClinicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinics")
@Tag(name = "Clínicas")
public class ClinicController {

    private final GetClinicProfileService profile;

    public ClinicController(GetClinicProfileService profile) {
        this.profile = profile;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público da clínica, horários e exames (UC05)")
    ClinicProfileResponse get(@PathVariable UUID id) {
        return ClinicProfileResponse.from(profile.getById(id));
    }
}
