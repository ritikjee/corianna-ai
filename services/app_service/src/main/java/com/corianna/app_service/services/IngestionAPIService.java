package com.corianna.app_service.services;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.IngestionAPI;
import com.corianna.app_service.entity.Member;
import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.repository.IngestionAPIRepository;

@Service
public class IngestionAPIService {

    private final IngestionAPIRepository ingestionAPIRepository;
    private final MemberService memberService;

    public IngestionAPIService(IngestionAPIRepository ingestionAPIRepository, MemberService memberService) {
        this.ingestionAPIRepository = ingestionAPIRepository;
        this.memberService = memberService;
    }

    @Cacheable(value = "ingestionAPI", key = "#ingestionAPIId")
    private IngestionAPI findIngestionAPIById(String ingestionAPIId) {
        return ingestionAPIRepository.findById(ingestionAPIId)
                .orElseThrow(() -> new IllegalArgumentException("Ingestion API not found"));
    }

    @Cacheable(value = "ingestionAPI", key = "#userId + #appId + #ingestionAPIId")
    public IngestionAPI getIngestionAPI(String userId, String appId, String ingestionAPIId) {
        memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        return findIngestionAPIById(ingestionAPIId);
    }

    @Cacheable(value = "ingestionAPI", key = "#userId + #appId")
    public List<IngestionAPI> getIngestionAPIs(String userId, String appId) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        return ingestionAPIRepository.findByAppId(member.getApp().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ingestion APIs not found"));

    }

    @CacheEvict(value = "ingestionAPI", key = "#userId + #appId")
    public IngestionAPI createIngestionAPI(String userId, String appId, String name) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("User does not have permission to create Ingestion API");
        }

        IngestionAPI ingestionAPI = new IngestionAPI();
        ingestionAPI.setApp(member.getApp());
        ingestionAPI.setName(name);
        ingestionAPI.setIsActive(true);

        return ingestionAPIRepository.save(ingestionAPI);
    }

    @Caching(evict = {
            @CacheEvict(value = "ingestionAPI", key = "#userId + #appId"),
            @CacheEvict(value = "ingestionAPI", key = "#userId + #appId + #ingestionAPIId")
    })
    public String deleteIngestionAPI(String userId, String appId, String ingestionAPIId) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("User does not have permission to delete Ingestion API");
        }

        IngestionAPI ingestionAPI = findIngestionAPIById(ingestionAPIId);

        ingestionAPIRepository.delete(ingestionAPI);

        return "Ingestion API deleted successfully";
    }

    public IngestionAPI updateIngestionAPIActiveStatus(String userId, String appId, String ingestionAPIId,
            Boolean isActive) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RoleEnum.MEMBER) {
            throw new IllegalArgumentException("User does not have permission to update Ingestion API");
        }

        IngestionAPI ingestionAPI = findIngestionAPIById(ingestionAPIId);

        ingestionAPI.setIsActive(isActive);

        return ingestionAPIRepository.save(ingestionAPI);

    }

}
