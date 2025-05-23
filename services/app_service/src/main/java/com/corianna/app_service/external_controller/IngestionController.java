package com.corianna.app_service.external_controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.app_service.dto.ResponseDTO;
import com.corianna.app_service.entity.App;
import com.corianna.app_service.entity.IngestionAPI;
import com.corianna.app_service.record.ManualIngestInput;
import com.corianna.app_service.record.ScrapeWebsiteMessage;
import com.corianna.app_service.repository.IngestionAPIRepository;
import com.corianna.app_service.services.AppService;
import com.corianna.app_service.utils.MessageProducer;
import com.corianna.app_service.utils.UserDisplayUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final AppService appService;
    private final MessageProducer messageProducer;
    private final IngestionAPIRepository ingestionAPIRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionController.class);

    public IngestionController(AppService appService, MessageProducer messageProducer,
            IngestionAPIRepository ingestionAPIRepository) {
        this.appService = appService;
        this.messageProducer = messageProducer;
        this.ingestionAPIRepository = ingestionAPIRepository;
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<?>> ingestNewURL(
            @RequestParam("api_key") String apiKey,
            @RequestParam("url") String url,
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

            IngestionAPI ingestionAPI = ingestionAPIRepository.findByApiKey(apiKey).orElseThrow(
                    () -> new IllegalArgumentException("Ingestion API not found with the provided API key"));

            String mode = url.contains("*") ? "pattern" : "single";

            ScrapeWebsiteMessage message = new ScrapeWebsiteMessage(url, mode,
                    ingestionAPI.getApp().getId(),
                    Map.of("ingestedBy", ingestionAPI.getName(),
                            "ingestedMedium", "API"));

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

    @PostMapping("/manual-ingest")
    public ResponseEntity<ResponseDTO<?>> ingestManually(@RequestBody ManualIngestInput input,
            HttpServletRequest request) {
        try {

            App app = appService.getApp(input.appId());

            if (app == null) {
                return ResponseEntity.badRequest().body(
                        new ResponseDTO<>("Bad Request", 400, null, "App not found", request.getRequestURI()));
            }

            String firstname = (String) request.getAttribute("firstname");
            String lastname = (String) request.getAttribute("lastname");
            String email = (String) request.getAttribute("email");
            String userDisplayName = UserDisplayUtil.formatUserDisplayName(firstname, lastname, email);

            for (String url : input.urls()) {

                String mode = url.contains("*") ? "pattern" : "single";

                ScrapeWebsiteMessage message = new ScrapeWebsiteMessage(url, mode,
                        app.getId(),
                        Map.of("ingestedBy", userDisplayName,
                                "ingestedMedium", "MANUAL"));

                messageProducer.sendMessage(message);
            }

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
