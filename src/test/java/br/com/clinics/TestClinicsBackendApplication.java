package br.com.clinics;

import org.springframework.boot.SpringApplication;

public class TestClinicsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClinicsBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
