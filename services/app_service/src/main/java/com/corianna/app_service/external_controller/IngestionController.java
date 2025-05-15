package com.corianna.app_service.external_controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.app_service.dto.ResponseDTO;
import com.corianna.app_service.entity.IngestionAPI;
import com.corianna.app_service.record.ScrapeWebsiteMessage;
import com.corianna.app_service.repository.IngestionAPIRepository;
import com.corianna.app_service.utils.MessageProducer;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final IngestionAPIRepository ingestionAPIRepository;
    private final MessageProducer messageProducer;

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionController.class);

    public IngestionController(IngestionAPIRepository ingestionAPIRepository, MessageProducer messageProducer) {
        this.ingestionAPIRepository = ingestionAPIRepository;
        this.messageProducer = messageProducer;
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<?>> ingestNewURL(
            @RequestParam("api_key") String apiKey,
            @RequestParam("url") String url,
            @RequestParam("mode") String mode,
            HttpServletRequest request) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResponseDTO<>("Bad Request", 400, null, "API key is missing", request.getRequestURI()));
            }

            if (url == null || url.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResponseDTO<>("Bad Request", 400, null, "URL is missing", request.getRequestURI()));
            }

            if (mode == null || mode.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ResponseDTO<>("Bad Request", 400, null, "Mode is missing", request.getRequestURI()));
            }
            if (!mode.equals("all") || !mode.equals("new")) {
                return ResponseEntity.badRequest().body(
                        new ResponseDTO<>("Bad Request", 400, null, "Mode should be either 'all' or 'new'",
                                request.getRequestURI()));

            }
            IngestionAPI ingestionAPI = ingestionAPIRepository.findByApiKey(apiKey).orElseThrow(
                    () -> new IllegalArgumentException("Ingestion API not found with the provided API key"));

            ScrapeWebsiteMessage message = new ScrapeWebsiteMessage(url, mode, ingestionAPI.getApp().getId());

            messageProducer.sendMessage(message);

            return ResponseEntity.ok().body(
                    new ResponseDTO<>("Success", 200, null, "Ingestion started successfully", request.getRequestURI()));

        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>("Bad Request", 400, null, e.getMessage(), request.getRequestURI()));

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }

    }

}
