package br.com.clinics.identity.application.port.out;

import java.util.UUID;

import br.com.clinics.identity.domain.AvatarImage;

public interface AvatarStorage {

    /** Grava a imagem e devolve o caminho público relativo (ex.: {@code /files/avatars/abc.png}). */
    String save(UUID userId, AvatarImage image);

    /** Remove um arquivo anterior; falhas não interrompem o fluxo. */
    void delete(String publicPath);
}
