package com.corianna.integration_service.controller.slack;

import org.springframework.web.bind.annotation.RestController;

import com.corianna.integration_service.config.JwtConfig;
import com.corianna.integration_service.controller.slack.actions.SlackActions;
import com.corianna.integration_service.dto.DataResponse;
import com.corianna.integration_service.dto.IntegrationRedirectURI;
import com.corianna.integration_service.dto.Member;
import com.corianna.integration_service.dto.MessageResponseDTO;
import com.corianna.integration_service.dto.SlackIntegrationInput;
import com.corianna.integration_service.dto.SlackOAuthResponse;
import com.corianna.integration_service.service.AppService;
import com.corianna.integration_service.service.SlackService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/slack")
public class SlackIntegrationController {

        @Value("${slack.client-id}")
        private String slackClientId;

        @Value("${secrets.jwt-secret}")
        private String jwtSecret;

        private final JwtConfig jwtConfig;
        private final AppService appService;
        private final SlackActions slackActions;
        private final SlackService slackService;

        public SlackIntegrationController(JwtConfig jwtConfig, AppService appService,
                        SlackActions slackActions, SlackService slackService) {
                this.jwtConfig = jwtConfig;
                this.appService = appService;
                this.slackActions = slackActions;
                this.slackService = slackService;
        }

        @PostMapping("/create-integration")
        public ResponseEntity<?> createSlackIntegration(@RequestBody SlackIntegrationInput input,
                        @NonNull HttpServletRequest request) {
                try {
                        String userId = request.getAttribute("userId").toString();
                        String appId = input.appId();

                        Member member = appService.getMemberInfo(userId, appId);

                        if (member == null) {
                                return ResponseEntity.badRequest().body(
                                                new MessageResponseDTO(
                                                                400,
                                                                "Member not found"));
                        }

                        String state = jwtConfig.encodeToken(jwtSecret, 86400000, Map.of(
                                        "userId", userId,
                                        "appId", appId));

                        String redirectUrl = String.format(
                                        "https://slack.com/oauth/v2/authorize?client_id=%s&scope=chat:write&scope=chat:write,channels:read,groups:read,mpim:read,im:read&user_scope=chat:write,channels:read,groups:read,mpim:read,im:read&state=%s&redirect_uri=https%%3A%%2F%%2Flocalhost%%3A3000%%2Fapi%%2Fauth%%2Fcallback%%2Fslack",
                                        slackClientId, state);

                        return ResponseEntity.ok().body(
                                        new DataResponse<IntegrationRedirectURI>(
                                                        200,
                                                        new IntegrationRedirectURI(redirectUrl)));

                } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        return ResponseEntity.badRequest().body(
                                        new MessageResponseDTO(
                                                        400,
                                                        e.getMessage()));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(
                                        new MessageResponseDTO(
                                                        500,
                                                        "An error occurred while creating the Slack integration"));
                }
        }

        @PostMapping("/callback")
        public ResponseEntity<?> authoriseCode(@RequestParam String code,
                        @RequestParam String state) {
                try {

                        Map<String, Object> encoded = jwtConfig.decodeToken(state, jwtSecret);

                        if (encoded == null) {
                                return ResponseEntity.badRequest().body(
                                                new MessageResponseDTO(
                                                                400,
                                                                "Invalid state"));

                        }

                        String appId = (String) encoded.get("appId");

                        if (appId == null) {
                                return ResponseEntity.badRequest().body(
                                                new MessageResponseDTO(
                                                                400,
                                                                "Invalid state"));
                        }

                        SlackOAuthResponse slackOAuthResponse = slackActions
                                        .getSlackOAuthResponseFromCode(code);

                        if (slackOAuthResponse == null || !slackOAuthResponse.isOk()) {
                                return ResponseEntity.badRequest().body(
                                                new MessageResponseDTO(
                                                                400,
                                                                "Invalid code"));
                        }

                        slackService.createSlackIntegration(appId, slackOAuthResponse);

                        return ResponseEntity.ok().body(
                                        new DataResponse<String>(200,
                                                        "Slack integration created successfully"));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(
                                        new MessageResponseDTO(
                                                        400,
                                                        e.getMessage()));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.badRequest().body(
                                        new MessageResponseDTO(
                                                        500,
                                                        "An error occurred while creating the Slack integration"));
                }

        }

}
