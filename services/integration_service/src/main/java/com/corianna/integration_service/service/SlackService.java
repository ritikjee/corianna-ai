package com.corianna.integration_service.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.corianna.integration_service.dto.SlackOAuthResponse;
import com.corianna.integration_service.entity.Slack;
import com.corianna.integration_service.repository.SlackRepository;

@Service
public class SlackService {

    private final SlackRepository slackRepository;

    public SlackService(SlackRepository slackRepository) {
        this.slackRepository = slackRepository;
    }

    @Cacheable(value = "slack", key = "#appId")
    public Slack createSlackIntegration(String appId, SlackOAuthResponse response) {
        Slack slack = new Slack();
        slack.setAppId(appId);
        slack.setSlackAppId(response.getAppId());
        slack.setSlackUserId(response.getAuthedUser().getId());
        slack.setUserToken(response.getAuthedUser().getAccessToken());
        slack.setAccessToken(response.getAccessToken());
        slack.setBotUserId(response.getBotUserId());
        slack.setTeamId(response.getTeam().getId());
        slack.setTeamName(response.getTeam().getName());

        return slackRepository.save(slack);
    }

}
