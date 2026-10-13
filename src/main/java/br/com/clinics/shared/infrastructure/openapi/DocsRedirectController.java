package br.com.clinics.shared.infrastructure.openapi;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Quem abre a raiz do servidor cai direto na documentação interativa (Swagger UI). */
@Controller
@Hidden
class DocsRedirectController {

    @GetMapping("/")
    String root() {
        return "redirect:/swagger-ui/index.html";
    }
}
