package com.mdd.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload used to create an article in a selected topic.
 */
public record CreatePostRequest(
        @NotNull(message = "Le theme est obligatoire")
        UUID topicId,

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 180, message = "Le titre ne doit pas depasser 180 caracteres")
        String title,

        @NotBlank(message = "Le contenu est obligatoire")
        String content
) {
}
