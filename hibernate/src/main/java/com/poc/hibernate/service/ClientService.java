package com.poc.hibernate.service;

import com.poc.hibernate.entity.Client;
import com.poc.hibernate.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Page<Client> getClients(
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("nom").ascending());

        return clientRepository.findAll(pageable);
    }


}
