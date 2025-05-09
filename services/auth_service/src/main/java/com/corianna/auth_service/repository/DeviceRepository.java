package com.corianna.auth_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.corianna.auth_service.dto.DeviceDTO;
import com.corianna.auth_service.entity.Device;

import jakarta.transaction.Transactional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

        Optional<Device> findBySessionId(String sessionId);

        @Modifying
        @Transactional
        @Query("DELETE FROM Device d WHERE d.sessionId = :sessionId")
        void deleteBySessionId(String sessionId);

        @Modifying
        @Transactional
        @Query("DELETE FROM Device d WHERE d.user.email = :email")
        void deleteAllByUsername(String email);

        @Modifying
        @Query("INSERT INTO Device (id, ipAddress, deviceType, os, deviceAgent, sessionId, user) " +
                        "VALUES (:id, :ipAddress, :deviceType, :os, :deviceAgent, :sessionId, " +
                        "(SELECT u FROM User u WHERE u.id = :userId))")
        void createDevice(@Param("id") String id,
                        @Param("ipAddress") String ipAddress,
                        @Param("deviceType") String deviceType,
                        @Param("os") String os,
                        @Param("deviceAgent") String deviceAgent,
                        @Param("sessionId") String sessionId,
                        @Param("userId") String userId);

        @Query("SELECT new com.corianna.auth_service.dto.DeviceDTO(d.id, d.ipAddress, d.deviceType, d.os, d.loginTime, d.deviceAgent) "
                        +
                        "FROM Device d WHERE d.user.email = :email")
        List<DeviceDTO> findAllByUsername(@Param("email") String email);

}