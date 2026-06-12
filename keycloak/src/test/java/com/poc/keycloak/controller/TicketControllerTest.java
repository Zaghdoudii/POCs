package com.poc.keycloak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.keycloak.dto.TicketRequest;
import com.poc.keycloak.dto.TicketResponse;
import com.poc.keycloak.exception.TicketNotFoundException;
import com.poc.keycloak.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    // Évite que @WebMvcTest essaie de contacter Keycloak (issuer-uri) au démarrage
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ─── Accès non authentifié → 401 ──────────────────────────────────────────

    @Test
    void findAll_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRequest("T", "D"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/tickets/1"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Rôle CLIENT : lecture seule ──────────────────────────────────────────

    @Test
    void findAll_returns200_forClient() throws Exception {
        when(ticketService.findAll()).thenReturn(List.of(new TicketResponse(1L, "Bug login", "Bloquant")));

        mockMvc.perform(get("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Bug login"));
    }

    @Test
    void findById_returns200_forClient() throws Exception {
        when(ticketService.findById(1L)).thenReturn(new TicketResponse(1L, "Bug login", "Bloquant"));

        mockMvc.perform(get("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isOk());
    }

    @Test
    void create_returns403_forClient() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRequest("T", "D"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_returns403_forClient() throws Exception {
        mockMvc.perform(put("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRequest("T", "D"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns403_forClient() throws Exception {
        mockMvc.perform(delete("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());
    }

    // ─── Rôle AGENT : lecture + création/modification, pas de suppression ─────

    @Test
    void create_returns201_forAgent() throws Exception {
        var request = new TicketRequest("Ticket AGENT", "Détails");
        when(ticketService.create(any())).thenReturn(new TicketResponse(1L, "Ticket AGENT", "Détails"));

        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns200_forAgent() throws Exception {
        var request = new TicketRequest("MAJ ticket", "Nouveau contenu");
        when(ticketService.update(eq(1L), any())).thenReturn(new TicketResponse(1L, "MAJ ticket", "Nouveau contenu"));

        mockMvc.perform(put("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("MAJ ticket"));
    }

    @Test
    void delete_returns403_forAgent() throws Exception {
        mockMvc.perform(delete("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isForbidden());
    }

    // ─── Rôle ADMIN : accès complet ───────────────────────────────────────────

    @Test
    void delete_returns204_forAdmin() throws Exception {
        doNothing().when(ticketService).delete(1L);

        mockMvc.perform(delete("/api/tickets/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void findById_returns404_forAdmin_whenNotFound() throws Exception {
        when(ticketService.findById(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns400_forAgent_whenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRequest("", "D"))))
                .andExpect(status().isBadRequest());
    }
}
