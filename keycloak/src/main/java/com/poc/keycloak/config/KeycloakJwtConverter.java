package com.poc.keycloak.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Keycloak places realm roles under realm_access.roles in the JWT, not in the standard "scope"
// claim that Spring Security reads by default — this converter bridges that gap.
//
// Keycloak JWT payload (relevant extract):
// {
//   "preferred_username": "agent-user",
//   "realm_access": { "roles": ["AGENT", "offline_access", "uma_authorization"] },
//   "resource_access": { "ticket-app": { "roles": ["..."] } }   // client roles (ignored here)
// }
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var scopeAuthorities = scopeConverter.convert(jwt);

        var authorities = Stream.concat(
                scopeAuthorities != null ? scopeAuthorities.stream() : Stream.empty(),
                extractRealmRoles(jwt).stream()
        ).collect(Collectors.toUnmodifiableSet());

        // preferred_username is the Keycloak username (e.g. "agent-user")
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }

    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
        if (realmAccess == null) return Set.of();

        var roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return Set.of();

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
