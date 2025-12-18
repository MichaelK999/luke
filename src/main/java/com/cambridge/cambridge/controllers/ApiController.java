package com.cambridge.cambridge.controllers;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.cambridge.cambridge.services.MapService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final MapService mapService;
    private final JdbcTemplate jdbcTemplate;
    // in memory stuff!
    private int requestCount = 0;
    private static final int MAX_REQUESTS = 100;

    public ApiController(MapService mapService, JdbcTemplate jdbcTemplate) {
        this.mapService = mapService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            String hashedPassword = hashPassword(password);
            String sql = "SELECT COUNT(*) FROM app_user WHERE username = ? AND password_hash = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, hashedPassword);

            if (count != null && count > 0) {
                Cookie authCookie = new Cookie("authenticated", "true");
                authCookie.setPath("/");
                authCookie.setMaxAge(3600);
                response.addCookie(authCookie);
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.ok(Map.of("success", false));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String adminPassword = body.get("adminpassword");

        if (adminPassword == null || !adminPassword.equals("curlbestcommand")) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        try {
            String hashedPassword = hashPassword(password);
            String sql = "INSERT INTO app_user (username, password_hash) VALUES (?, ?)";
            jdbcTemplate.update(sql, username, hashedPassword);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    private static final String GOOGLE_GEOCODING_API_KEY = "AIzaSyB6NutZ5_Ad0fip0RTUYa8yqkg2cV2KG6o";
    private static final String GOOGLE_GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    @PostMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocode(@RequestBody Map<String, String> body) {
        // increment requestCount, put this in so that when running in production no one spams our api key
        // yea it's in memory, so will reset whenever server is relaunched but good enough with the free plan on google!
        requestCount++;

        if (requestCount > MAX_REQUESTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Rate limit exceeded. Max 100 requests per instance."));
        }
        System.out.println("Someone hit our API! (Request #" + requestCount + ")");
        String original = body.get("original");
        String destination = body.get("destination");

        System.out.println("=== Geocode Request ===");
        System.out.println("Original: " + original);
        System.out.println("Destination: " + destination);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Geocode origin
            String url1 = GOOGLE_GEOCODING_URL + "?address=" + original + "&key=" + GOOGLE_GEOCODING_API_KEY;
            String response1 = restTemplate.getForObject(url1, String.class);
            JsonNode root1 = mapper.readTree(response1);

            JsonNode results1 = root1.get("results");
            if (results1 == null || results1.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Could not find location for origin: " + original,
                    "status", root1.has("status") ? root1.get("status").asText() : "UNKNOWN"
                ));
            }

            double lat1 = results1.get(0).get("geometry").get("location").get("lat").asDouble();
            double lng1 = results1.get(0).get("geometry").get("location").get("lng").asDouble();

            // Geocode destination
            String url2 = GOOGLE_GEOCODING_URL + "?address=" + destination + "&key=" + GOOGLE_GEOCODING_API_KEY;
            String response2 = restTemplate.getForObject(url2, String.class);
            JsonNode root2 = mapper.readTree(response2);

            JsonNode results2 = root2.get("results");
            if (results2 == null || results2.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Could not find location for destination: " + destination,
                    "status", root2.has("status") ? root2.get("status").asText() : "UNKNOWN"
                ));
            }

            double lat2 = results2.get(0).get("geometry").get("location").get("lat").asDouble();
            double lng2 = results2.get(0).get("geometry").get("location").get("lng").asDouble();

            System.out.println("=== Geocode Results!!!!! ===");
            System.out.println("Original - Lat: " + lat1 + ", Lng: " + lng1);
            System.out.println("Destination - Lat: " + lat2 + ", Lng: " + lng2);

            // Run Dijkstra and get the route
            List<double[]> routeCoords = mapService.calculateRoute(lat1, lng1, lat2, lng2);

            // Convert to list of {lat, lng} maps for JSON
            List<Map<String, Double>> route = new ArrayList<>();
            for (double[] coord : routeCoords) {
                Map<String, Double> point = new HashMap<>();
                point.put("lat", coord[0]);
                point.put("lng", coord[1]);
                route.add(point);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("original", Map.of("lat", lat1, "lng", lng1));
            response.put("destination", Map.of("lat", lat2, "lng", lng2));
            response.put("route", route);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Error during geocoding: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }
}
