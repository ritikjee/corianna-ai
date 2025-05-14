package com.corianna.app_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.corianna.app_service.entity.Member;

@Repository
public interface MembersRepository extends JpaRepository<Member, String> {

    Optional<List<Member>> findByUserId(String userId);

    @Transactional
    @Modifying
    @Query("""
            DELETE FROM App a
            WHERE a.id = (
                SELECT m.app.id
                FROM Member m
                WHERE m.userId = :userId
                AND m.app.id = :appId
                AND m.role = 0
            )
            """)
    void deleteAppIfMemberIsOwner(@Param("appId") String appId, @Param("userId") String userId);

    Optional<Member> findByUserIdAndAppId(String userId, String appId);

}
