package com.corianna.auth_service.services;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.corianna.auth_service.dto.DeviceDTO;
import com.corianna.auth_service.entity.Device;
import com.corianna.auth_service.repository.DeviceRepository;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public DeviceService(DeviceRepository deviceRepository, RedisTemplate<String, String> redisTemplate) {
        this.deviceRepository = deviceRepository;
        this.redisTemplate = redisTemplate;
    }

    @Cacheable(value = "device", key = "#sessionId")
    public Device findDeviceBySessionId(String sessionId) {
        return deviceRepository.findBySessionId(sessionId).orElse(null);
    }

    @CacheEvict(value = "device", key = "#sessionId")
    public void removeDevice(String sessionId) {
        redisTemplate.opsForValue().set("exp_sess::" + sessionId, "removed");
        deviceRepository.deleteBySessionId(sessionId);
    }

    @Caching(evict = {
            @CacheEvict(value = "device", key = "#sessionId"),
            @CacheEvict(value = "devices", key = "#email")
    })
    public void logoutAllDevices(String email, String sessionId) {
        // TODO: remove all devices from redis
        deviceRepository.deleteAllByUsername(email);
    }

    @Cacheable(value = "devices", key = "#email")
    public List<DeviceDTO> getAllDevices(String email) {
        return deviceRepository.findAllByUsername(email);
    }

}