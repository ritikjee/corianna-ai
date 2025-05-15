package com.corianna.app_service.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.corianna.app_service.entity.App;
import com.corianna.app_service.record.CreateAppInput;
import com.corianna.app_service.services.AppService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AppController {

    private final AppService appService;
    private final HttpServletRequest request;

    public AppController(AppService appService, HttpServletRequest request) {
        this.appService = appService;
        this.request = request;
    }

    @QueryMapping
    public List<App> getApps() {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        return appService.getApps(userId);
    }

    @MutationMapping
    public App createApp(@Argument("input") CreateAppInput input) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        return appService.createApp(input.name(), input.url(), userId);
    }

    @MutationMapping
    public String deleteApp(@Argument("appId") String appId) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        appService.deleteApp(appId, userId);

        return "App deleted successfully for App ID: " + appId;
    }

}
