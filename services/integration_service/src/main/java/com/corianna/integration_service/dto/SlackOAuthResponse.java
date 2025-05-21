package com.corianna.integration_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SlackOAuthResponse {
    private boolean ok;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    private String scope;

    @JsonProperty("bot_user_id")
    private String botUserId;

    @JsonProperty("app_id")
    private String appId;

    private Team team;
    private Enterprise enterprise;

    @JsonProperty("authed_user")
    private AuthedUser authedUser;

    @Data
    public static class Team {
        private String name;
        private String id;
    }

    @Data
    public static class Enterprise {
        private String name;
        private String id;
    }

    @Data
    public static class AuthedUser {
        private String id;
        private String scope;

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;
    }
}
