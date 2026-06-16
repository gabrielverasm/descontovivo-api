package br.com.descontovivo.engagement.api;

public record PromotionVoteResponse(
        int likesCount,
        int dislikesCount,
        String userVote
) {}
