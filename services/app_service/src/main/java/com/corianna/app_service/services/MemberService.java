package com.corianna.app_service.services;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.Member;
import com.corianna.app_service.repository.MembersRepository;

@Service
public class MemberService {

    private final MembersRepository memberRepository;

    public MemberService(MembersRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Cacheable(value = "member", key = "#userId + #appId")
    public Optional<Member> getMemberInfo(String userId, String appId) {
        return memberRepository.findByUserIdAndAppId(userId, appId);
    }
}
