package com.corianna.app_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.corianna.app_service.entity.Integration;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, String> {

}
