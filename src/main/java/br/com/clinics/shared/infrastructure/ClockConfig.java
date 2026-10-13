package br.com.clinics.shared.infrastructure;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Recife");

    /** Relógio injetável: permite fixar o tempo nos testes das regras que dependem de data e hora. */
    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
