package br.com.clinics.identity.domain;

import br.com.clinics.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarImageTest {

    private static byte[] withPrefix(int size, int... prefix) {
        byte[] data = new byte[size];
        for (int i = 0; i < prefix.length; i++) {
            data[i] = (byte) prefix[i];
        }
        return data;
    }

    @Test
    void detectsPngByMagicBytes() {
        AvatarImage image = AvatarImage.of(withPrefix(100, 0x89, 'P', 'N', 'G'));
        assertThat(image.extension()).isEqualTo("png");
        assertThat(image.contentType()).isEqualTo("image/png");
    }

    @Test
    void detectsJpegByMagicBytes() {
        AvatarImage image = AvatarImage.of(withPrefix(100, 0xFF, 0xD8, 0xFF, 0xE0));
        assertThat(image.extension()).isEqualTo("jpg");
    }

    @Test
    void acceptsExactlyTwoMegabytesAndRejectsOneByteMore() {
        assertThat(AvatarImage.of(withPrefix(AvatarImage.MAX_BYTES, 0xFF, 0xD8, 0xFF)).bytes()).hasSize(AvatarImage.MAX_BYTES);
        assertThatThrownBy(() -> AvatarImage.of(withPrefix(AvatarImage.MAX_BYTES + 1, 0xFF, 0xD8, 0xFF)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOtherFormatsAndEmptyFiles() {
        assertThatThrownBy(() -> AvatarImage.of("GIF89a......".getBytes())).hasFieldOrPropertyWithValue("code", "INVALID_AVATAR");
        assertThatThrownBy(() -> AvatarImage.of(new byte[0])).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> AvatarImage.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> AvatarImage.of(new byte[]{(byte) 0xFF})).isInstanceOf(ValidationException.class);
    }
}
