package com.poc.keycloak.service;

import com.poc.keycloak.dto.TicketRequest;
import com.poc.keycloak.entity.Ticket;
import com.poc.keycloak.exception.TicketNotFoundException;
import com.poc.keycloak.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void findById_returnsResponse_whenExists() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, "Fix bug", "Urgent")));

        var result = ticketService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Fix bug");
    }

    @Test
    void findById_throws_whenNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findById(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesAndReturnsResponse() {
        var request = new TicketRequest("New ticket", "Details");
        when(ticketRepository.save(any())).thenReturn(ticket(1L, "New ticket", "Details"));

        var result = ticketService.create(request);

        assertThat(result.title()).isEqualTo("New ticket");
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void delete_throws_whenNotFound() {
        when(ticketRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.delete(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void delete_callsDeleteById_whenExists() {
        when(ticketRepository.existsById(1L)).thenReturn(true);

        ticketService.delete(1L);

        verify(ticketRepository).deleteById(1L);
    }

    @Test
    void findAll_returnsMappedList() {
        when(ticketRepository.findAll()).thenReturn(List.of(
                ticket(1L, "T1", "D1"),
                ticket(2L, "T2", "D2")
        ));

        var result = ticketService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("title").containsExactly("T1", "T2");
    }

    private static Ticket ticket(Long id, String title, String description) {
        var t = new Ticket();
        t.setId(id);
        t.setTitle(title);
        t.setDescription(description);
        return t;
    }
}
