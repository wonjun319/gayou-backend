package com.gayou.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/api/hello")
    public String test() {
        return "Hello, world!";
    }

    @PostMapping("/route")
    public ResponseEntity<String> receiveRouteData(@RequestBody Map<String, Object> routeData) {
        System.out.println("routeData: " + routeData);

        // region, neighborhoods, age, gender, travelDate, isLocal, transport,
        // selectedConcepts
        String region = (String) routeData.get("region");
        System.out.println(region);

        return ResponseEntity.ok("successfully");
    }
}