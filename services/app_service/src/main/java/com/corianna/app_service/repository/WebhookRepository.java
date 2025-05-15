package com.corianna.app_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.corianna.app_service.entity.Webhook;

public interface WebhookRepository extends JpaRepository<Webhook, String> {

    Optional<List<Webhook>> findByAppId(String appId);

}
