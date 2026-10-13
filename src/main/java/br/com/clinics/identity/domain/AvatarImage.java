package br.com.clinics.identity.domain;

import br.com.clinics.shared.domain.ValidationException;

/** RN34: somente JPEG ou PNG de até 2 MB. O tipo é detectado pelos bytes, não pelo cabeçalho informado pelo cliente. */
public record AvatarImage(byte[] bytes, String extension, String contentType) {

    public static final int MAX_BYTES = 2 * 1024 * 1024;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public static AvatarImage of(byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            throw invalid();
        }
        if (startsWith(data, PNG)) {
            return new AvatarImage(data, "png", "image/png");
        }
        if (startsWith(data, JPEG)) {
            return new AvatarImage(data, "jpg", "image/jpeg");
        }
        throw invalid();
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static ValidationException invalid() {
        return new ValidationException("INVALID_AVATAR", "Envie uma imagem JPEG ou PNG de até 2 MB.");
    }
}
