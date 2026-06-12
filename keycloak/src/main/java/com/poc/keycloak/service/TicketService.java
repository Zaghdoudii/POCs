package com.poc.keycloak.service;

import com.poc.keycloak.dto.TicketRequest;
import com.poc.keycloak.dto.TicketResponse;

import java.util.List;

public interface TicketService {

    TicketResponse findById(Long id);

    TicketResponse create(TicketRequest request);

    TicketResponse update(Long id, TicketRequest request);

    void delete(Long id);

    List<TicketResponse> findAll();
}
