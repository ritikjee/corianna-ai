package com.corianna.app_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.corianna.app_service.entity.IngestionAPI;

@Repository
public interface IngestionAPIRepository extends JpaRepository<IngestionAPI, String> {

    Optional<List<IngestionAPI>> findByAppId(String appId);

    Optional<IngestionAPI> findByApiKey(String apiKey);

}