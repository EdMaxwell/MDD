package com.mdd.post.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload used to add a comment to an article.
 */
public record CreateCommentRequest(
        @NotBlank(message = "Le contenu du commentaire est obligatoire")
        String content
) {
}
