package com.corianna.integration_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.corianna.integration_service.entity.Slack;

@Repository
public interface SlackRepository extends JpaRepository<Slack, String> {

}
