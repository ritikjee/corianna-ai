package com.corianna.app_service.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.Member;
import com.corianna.app_service.entity.Webhook;
import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.record.CreateWebhookInput;
import com.corianna.app_service.record.KeyValue;
import com.corianna.app_service.repository.WebhookRepository;

@Service
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final MemberService memberService;

    public WebhookService(WebhookRepository webhookRepository, MemberService memberService) {
        this.webhookRepository = webhookRepository;
        this.memberService = memberService;
    }

    @Cacheable(value = "webhook", key = "#webhookId")
    private Webhook findWebhookById(String webhookId) {
        return webhookRepository.findById(webhookId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found"));
    }

    @Cacheable(value = "webhook", key = "#userId" + "#appId")
    public List<Webhook> getWebhooks(String userId, String appId) {
        memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        return webhookRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found"));
    }

    public Webhook getWebhookById(String userId, String appId, String webhookId) {
        memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        return findWebhookById(webhookId);
    }

    @CacheEvict(value = "webhook", key = "#userId" + "#input.appId()")
    public Webhook createWebhook(String userId, CreateWebhookInput input) {
        Member member = memberService.getMemberInfo(userId, input.appId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("Member does not have permission to create webhook");
        }

        Webhook webhook = new Webhook();
        webhook.setName(input.name());
        webhook.setUrl(input.url());
        webhook.setToken(input.token());

        Map<String, String> headers;

        if (input.headers() != null) {
            headers = input.headers().stream()
                    .collect(Collectors.toMap(KeyValue::key, KeyValue::value));
        } else {
            headers = Map.of();
        }

        webhook.setHeaders(headers);
        webhook.setActive(true);
        webhook.setApp(member.getApp());

        Webhook savedWebhook = webhookRepository.save(webhook);
        return savedWebhook;
    }

    @CacheEvict(value = "webhook", allEntries = true)
    public String deleteWebhook(String userId, String appId, String webhookId) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("Member does not have permission to delete webhook");
        }

        Webhook webhook = findWebhookById(webhookId);

        webhookRepository.delete(webhook);
        return "Webhook deleted successfully";
    }

    @Caching(evict = {
            @CacheEvict(value = "webhook", key = "#userId" + "#appId"),
            @CacheEvict(value = "webhook", key = "#userId" + "#appId" + "#webhookId")
    })
    public Webhook updateWebhook(String userId, String appId, String webhookId, Boolean isActive) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("Member does not have permission to update webhook");
        }

        Webhook webhook = findWebhookById(webhookId);

        if (isActive != null) {
            webhook.setActive(isActive);
        }
        Webhook updatedWebhook = webhookRepository.save(webhook);
        return updatedWebhook;
    }

    @Cacheable(value = "webhooks", key = "#appId")
    public List<Webhook> getWebhooks(String appId) {
        return webhookRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found"));
    }

}
