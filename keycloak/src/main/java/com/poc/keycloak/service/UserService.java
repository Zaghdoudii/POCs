package com.poc.keycloak.service;

import com.poc.keycloak.dto.UserProfileResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    public UserProfileResponse getCurrentUser(Jwt jwt) {

        String username =
                jwt.getClaimAsString("preferred_username");

        String email =
                jwt.getClaimAsString("email");

        Map<String, Object> realmAccess =
                jwt.getClaimAsMap("realm_access");

        assert realmAccess != null;
        List<String> roles = (List<String>) realmAccess.get("roles");

        return new UserProfileResponse(username, email, roles);
    }
}