package br.com.clinics.engagement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import br.com.clinics.shared.domain.ValidationException;

/** RN28: {@code https://wa.me/{número}?text={mensagem codificada}}. */
public final class WhatsAppLinkBuilder {

    private WhatsAppLinkBuilder() {
    }

    public static URI build(String whatsappNumber, String examName, BigDecimal price) {
        String digits = whatsappNumber == null ? "" : whatsappNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new ValidationException("CLINIC_WITHOUT_WHATSAPP", "A clínica não informou um WhatsApp.");
        }
        String formattedPrice = price.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        String message = "Olá! Vi o exame %s por R$ %s no Clinics e gostaria de agendar.".formatted(examName, formattedPrice);
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create("https://wa.me/" + digits + "?text=" + encoded);
    }
}
