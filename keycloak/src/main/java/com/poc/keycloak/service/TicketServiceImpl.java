package com.poc.keycloak.service;

import com.poc.keycloak.dto.TicketRequest;
import com.poc.keycloak.dto.TicketResponse;
import com.poc.keycloak.entity.Ticket;
import com.poc.keycloak.exception.TicketNotFoundException;
import com.poc.keycloak.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    public TicketResponse findById(Long id) {
        return ticketRepository.findById(id)
                .map(TicketResponse::from)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Override
    @Transactional
    public TicketResponse create(TicketRequest request) {
        var ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        var ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        // Dirty checking — Hibernate flushes the UPDATE automatically at commit
        return TicketResponse.from(ticket);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        ticketRepository.deleteById(id);
    }

    @Override
    public List<TicketResponse> findAll() {
        return ticketRepository.findAll().stream()
                .map(TicketResponse::from)
                .toList();
    }
}
