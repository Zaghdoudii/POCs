package com.poc.keycloak.dto;

import java.util.List;

public record UserProfileResponse(
        String username,
        String email,
        List<String> roles
) {
}