package com.corianna.app_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.corianna.app_service.entity.Member;

@Repository
public interface MembersRepository extends JpaRepository<Member, String> {

}
