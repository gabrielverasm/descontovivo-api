package br.com.descontovivo.engagement.api;

public record CommentModerationRequest(
        String action,
        String reason
) {}
