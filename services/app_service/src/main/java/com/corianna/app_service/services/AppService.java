package com.corianna.app_service.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.App;
import com.corianna.app_service.entity.Member;
import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.record.ScrapeWebsiteMessage;
import com.corianna.app_service.repository.AppRepository;
import com.corianna.app_service.repository.MembersRepository;
import com.corianna.app_service.utils.MessageProducer;

import jakarta.transaction.Transactional;

@Service
public class AppService {

    private final AppRepository appRepository;
    private final MembersRepository membersRepository;
    private final MessageProducer messageProducer;

    public AppService(AppRepository appRepository, MembersRepository membersRepository,
            MessageProducer messageProducer) {
        this.appRepository = appRepository;
        this.membersRepository = membersRepository;
        this.messageProducer = messageProducer;
    }

    @Transactional
    @CacheEvict(value = "apps", key = "#userId")
    public App createApp(String appName, String websiteUrl, String userId) {
        App app = new App();

        app.setName(appName);
        app.setUrl(websiteUrl);

        App newApp = appRepository.save(app);

        Member member = new Member();

        member.setUserId(userId);
        member.setApp(newApp);
        member.setRole(RoleEnum.OWNER);

        membersRepository.save(member);

        ScrapeWebsiteMessage message = new ScrapeWebsiteMessage(websiteUrl, "full", newApp.getId(), Map.of());

        messageProducer.sendMessage(message);

        return newApp;
    }

    @Cacheable(value = "apps", key = "#userId")
    public List<App> getApps(String userId) {

        return membersRepository
                .findByUserId(userId)
                .orElse(null)
                .stream()
                .map(Member::getApp)
                .collect(Collectors.toList());

    }

    @CacheEvict(value = "apps", key = "#userId")
    public void deleteApp(String appId, String userId) {
        membersRepository.deleteAppIfMemberIsOwner(appId, userId);
        appRepository.deleteById(appId);
    }

}
